#!/usr/bin/python
# -*- coding: UTF-8 -*-

import socket
import requests
import json
import argparse
import os
import re

if __name__ == '__main__':
    """
    The following DNS records will be created
        <prefix>-v4.<domain> for IPv4
        <prefix>-v6.<domain> for IPv6
    IP address depends on the result of ifconfig command
    """
    parser = argparse.ArgumentParser()
    parser.add_argument('--domain', required=True, help='which means zone in cloudflare. eg: example.com')
    parser.add_argument('--prefix', required=True, help='which means prefix of DDNS')
    parser.add_argument('--api-key', required=True, help='the api key of cloudflare')
    arg = parser.parse_args()
    domain = arg.domain
    dns_base_name = arg.prefix
    cf_api_key = arg.api_key

    print('Domain:', domain)
    print('DNS base name:', dns_base_name)

    cf_base_url = 'https://api.cloudflare.com/client/v4'

    # grep of interface name
    interface_name = [
        'wan'
    ]

    # blacklist of IPs
    blacklist = [
        '^fe80',
        '^fd19',
        '^::',
        '^127',
        '^192',
        '^172',
        '^10\.'
    ]

    ifconfig = os.popen("ifconfig -a").read()
    interfaces = re.findall('([a-zA-Z0-9_-]+)\s+Link([\w\W]*?)(?:\n\n|$)',ifconfig)

    ipv4 = []
    ipv6 = []
    for inet in interfaces:
        inet_name = inet[0]
        to_continue = False
        for white in interface_name:
            if re.search(white, inet_name):
                to_continue = True
        
        if not to_continue:
            continue

        address0 = inet[1]
        address1 = address = re.findall('addr:\s*([0-9a-f.:]+)', address0)
        for addr in address:
            to_continue = True
            for b in blacklist:
                if re.search(b, addr):
                    to_continue = False
            
            if not to_continue:
                continue

            if addr.__contains__(':'):
                print('IPv6(%s) With Interface(%s)' % (addr, inet_name))
                ipv6.append(addr)
            else:
                print('IPv4(%s) With Interface(%s)' % (addr, inet_name))
                ipv4.append(addr)

    headers = {
        'Authorization': 'Bearer %s' % cf_api_key,
        'Content-Type': 'application/json'
    }

    zones = requests.get('%s/zones' % cf_base_url,
                         headers=headers,
                         params={'match': 'all', 'name': domain}).json()

    zone_id = zones['result'][0]['id']
    print('find zone id %s for %s' % (zone_id, domain))
    dns_records = requests.get('%s/zones/%s/dns_records' % (cf_base_url, zone_id),
                               params={
                                   'per_page': '100',
                                   'proxied': False
                               },
                               headers=headers).json()
    # print('raw list:', dns_records)
    for record in dns_records['result']:
        dns_id = record['id']
        name = record['name']
        content = record['content']
        if name.startswith(dns_base_name):
            # delete records if exist
            if content not in ipv4 and content not in ipv6:
                requests.delete('%s/zones/%s/dns_records/%s' % (cf_base_url, zone_id, dns_id),
                                headers=headers)
                print('delete id(%s) for %s with %s' % (dns_id, name, content))
            else:
                print('exist DNS record with id(%s) name(%s) content(%s)' % (dns_id, name, content))
                if content in ipv6:
                    ipv6.remove(content)
                if content in ipv4:
                    ipv4.remove(content)

    create_dns_record = []
    # create new records
    for v4 in ipv4:
        ret = requests.post('%s/zones/%s/dns_records' % (cf_base_url, zone_id),
                            headers=headers,
                            data=json.dumps({
                                'type': 'A',
                                'name': '%s-v4.%s' % (dns_base_name, domain),
                                'content': v4,
                                'ttl': '120',
                                'proxied': False
                            })).json()
        create_dns_record.append(ret)
    for v6 in ipv6:
        ret = requests.post('%s/zones/%s/dns_records' % (cf_base_url, zone_id),
                            headers=headers,
                            data=json.dumps({
                                'type': 'AAAA',
                                'name': '%s-v6.%s' % (dns_base_name, domain),
                                'content': v6,
                                'ttl': '120',
                                'proxied': False
                            })).json()
        create_dns_record.append(ret)

    for rec in create_dns_record:
        if rec['success']:
            ret = rec['result']
            dns_id = ret['id']
            dns_name = ret['name']
            dns_content = ret['content']
            print('add new record(%s) id(%s)  with content(%s)' % (dns_name, dns_id, dns_content))
        else:
            print('Error:', rec)

    print('all done.')

