# Welcome to your CDK Java project!

This is a blank project for CDK development with Java.

The `cdk.json` file tells the CDK Toolkit how to execute your app.

It is a [Maven](https://maven.apache.org/) based project, so you can open this project with any Maven compatible Java IDE to build and run tests.

## Useful commands

 * `mvn package`     compile and run tests
 * `cdk ls`          list all stacks in the app
 * `cdk synth`       emits the synthesized CloudFormation template
 * `cdk deploy`      deploy this stack to your default AWS account/region
 * `cdk diff`        compare deployed stack with current state
 * `cdk docs`        open CDK documentation
 * `cdk list`	     show all stacks
 * `cdk destroy VPC CLUSTER RDS SERVICE01 SNS01 SERVICE02 DYNAMODB`
 * `cdk deploy --parameters RDS:databasePassword=exemploEstudo1301 RDS CLUSTER SERVICE01 VPC SNS01 SERVICE02 DYNAMODB`
 * `cdk deploy --all --parameters RDS:databasePassword=exemploEstudo1301`
 * `cdk deploy --all --parameters RDS:databasePassword=exemploEstudo1301  --require-approval never`
 * `for deploy use --require-approval never, for destroy use --force`
 * `ab -n 500 -c 20 http://SERVI-ALB02-19A5W94BZHPQU-820958961.us-east-1.elb.amazonaws.com:9090/api/events`
 * `ab -n 500 -c 20 http://SERVI-ALB02-19A5W94BZHPQU-820958961.us-east-1.elb.amazonaws.com:9090/api/events/ref10/PRODUCT_UPDATE`

Enjoy!
renato@likwi.com.br
