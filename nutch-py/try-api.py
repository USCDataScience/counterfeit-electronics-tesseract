from nutch.nutch import Server
import nutch

header = {'Content-Type': 'application/json'}
nutch_server = Server('http://localhost:8081', False)

# get the list of all jobs
data = nutch_server.call('get','/job',headers=header)

# get the list of available configurations
data = nutch_server.call('get','/config',headers=header)

# create a new seedlist
values = {'name':'seed_list_1', 'seedUrls' : ['http://espn.com','http://espn.go.com']}
data = nutch_server.call('post','/seed/create',data=values,headers=header)

# access the information stored in the crawldb
values = { 'type' : 'stats', 'confId' : 'default', 'crawlId' : 'test', 'args' : {'topN':'100'} }
data = nutch_server.call('post', '/db/crawldb', data=values, headers=header)
