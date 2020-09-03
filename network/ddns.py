#!/usr/bin/python
# -*- coding: UTF-8 -*-

import socket
import requests
import json
import argparse

if __name__ == '__main__':
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

    blacklist = [
        'fe80',
        '192.168',
        '172.17'
    ]

    address = socket.getaddrinfo(socket.gethostname(), None)

    ipv4 = []
    ipv6 = []
    for addr in address:
        ip = addr[4][0]
        flag = True
        for b in blacklist:
            if ip.startswith(b):
                flag = False

        if not flag:
            continue
        if addr[0] == socket.AddressFamily.AF_INET6:
            print('IPv6:', ip)
            ipv6.append(ip)
        elif addr[0] == socket.AddressFamily.AF_INET:
            print('IPv4:', ip)
            ipv4.append(ip)
        else:
            print('un-support address:', addr)

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
                               headers=headers).json()

    for record in dns_records['result']:
        dns_id = record['id']
        name = record['name']
        content = record['content']
        if name.startswith(dns_base_name):
            # delete records if exist
            if content not in ipv4 and content not in ipv6:
                requests.delete('%s/zones/%s/dns_records/%s' % (cf_base_url, zone_id, dns_id),
                                headers=headers)
                ipv6.remove(content)
                ipv4.remove(content)
                print('delete id(%s) for %s with %s' % (dns_id, name, content))

    create_dns_record = []
    # create new records
    for v4 in ipv4:
        ret = requests.post('%s/zones/%s/dns_records' % (cf_base_url, zone_id),
                            headers=headers,
                            data=json.dumps({
                                'type': 'A',
                                'name': '%s-v4.%s' % (dns_base_name, domain),
                                'content': v4,
                                'ttl': '2',
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
                                'ttl': '2',
                                'proxied': False
                            }))
        create_dns_record.append(ret)

    for rec in create_dns_record:
        if rec['success']:
            ret = rec['result']
            dns_id = ret['id']
            dns_name = ret['name']
            dns_content = ret['content']
            print('add new record(%s) id(%s)  with content(%s)' % (dns_name, dns_id, dns_content))
        else:
            print('Error:%s', rec)

    print('all done.')
