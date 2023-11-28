package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.ec2.Vpc;
import software.constructs.Construct;
// import software.amazon.awscdk.Duration;
// import software.amazon.awscdk.services.sqs.Queue;


public class VPCStack extends Stack {
    private Vpc vpc;
    public VPCStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public VPCStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        vpc = Vpc.Builder
                .create(this, "VPC")
                .maxAzs(3) //availability zone
                .build();

    }

    public Vpc getVPC() {
        return vpc;
    }
}
