# AWS Kubernetes STG, PROD 구성정보

## AWS Environments

**OS** : ubuntu 16.04.1 LTS

### STG

Bearer Token  -> 2950c2.e8237a50df67854b

Master Dashboard -> http://13.124.31.81:32719

Api Server : https://13.124.31.81:6443

| Type   | Public IP     | Private IP    |
| ------ | ------------- | ------------- |
| master | 13.124.31.81  | 10.2.14.154   |
| slave1 | 13.124.19.160 | 10.2.3.117    |
| slave2 | 13.124.23.229 | 10.2.5.98     |


### PROD

Bearer Token  - > d1db80.762032854eff7b1f

Master Dashboard -> http://13.124.1.27:30156

| Type   | Public IP     | Private IP    |
| ------ | ------------- | ------------- |
| master | 13.124.1.27   | 10.3.1.139    |
| slave1 | 13.124.1.35   | 10.3.1.183    |
| slave2 | 13.124.12.246 | 10.3.1.95     |
| slave3 | 13.124.13.16  | 10.3.1.229    |
| slave4 | 13.124.0.80   | 10.3.1.38     |
| slave5 | 13.124.6.59   | 10.3.1.106    |