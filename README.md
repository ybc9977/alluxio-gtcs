
Alluxio for Game Theoretical Communication System
=======

Built atop Alluxio 1.8.0, by ybc in Hong Kong University of Science and Technology.
When setting up the system, configuration should be done: (under Alluxio Home)
```
$ cp conf/alluxio-site.properties.template conf/alluxio-site.properties

1. localhost
$ echo "alluxio.master.hostname=localhost" >> conf/alluxio-site.properties

2. AWS
$ echo "aws.accessKeyId=<AWS_ACCESS_KEY_ID>" >> conf/alluxio-site.properties
$ echo "aws.secretKey=<AWS_SECRET_ACCESS_KEY>" >> conf/alluxio-site.properties

```

