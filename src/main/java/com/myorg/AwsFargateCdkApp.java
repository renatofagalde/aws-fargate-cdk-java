package com.myorg;

import software.amazon.awscdk.App;

public class AwsFargateCdkApp {
    public static void main(final String[] args) {
        App app = new App();

        final VPCStack vpcStack = new VPCStack(app, "VPC");
        final ClusterStack clusterStack = new ClusterStack(app, "CLUSTER", vpcStack.getVPC());
        clusterStack.addDependency(vpcStack);

        final RDSStack rdsStack = new RDSStack(app, "RDS", vpcStack.getVPC());
        rdsStack.addDependency(vpcStack);

        final SNSStack snsStack = new SNSStack(app, "SNS01");
        final FileStack file01 = new FileStack(app, "FILE01");

        final Service01Stack service01Stack = new Service01Stack(app, "SERVICE01", clusterStack.getCluster(),
                snsStack.getProductEventsTopic(),file01.getBucket(),file01.getFileQueue());
        service01Stack.addDependency(clusterStack);
        service01Stack.addDependency(rdsStack);
        service01Stack.addDependency(snsStack);
        service01Stack.addDependency(file01);

        DynamoDBStack dynamoDBStack = new DynamoDBStack(app, "DYNAMO01");

        final Service02Stack service02Stack = new Service02Stack(app, "SERVICE02",
                clusterStack.getCluster(), snsStack.getProductEventsTopic(),dynamoDBStack.getProductEventTable());
        service02Stack.addDependency(clusterStack);
        service02Stack.addDependency(snsStack);
        service02Stack.addDependency(dynamoDBStack);

        app.synth();
    }
}

