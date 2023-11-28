package com.myorg;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.constructs.Construct;

public class DynamoDBStack extends Stack {
    private Table productEventTable;

    public DynamoDBStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public DynamoDBStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);
        this.productEventTable = Table.Builder.create(this, "productEventTable")
                .tableName("product-event")
                .billingMode(BillingMode.PROVISIONED) //forma de cobranca
                .readCapacity(1) // 1 unidade por segundo
                .writeCapacity(1)
                .partitionKey(Attribute.builder()
                        .name("pk") //partionkey
                        .type(AttributeType.STRING)
                        .build())
                .sortKey(Attribute.builder()
                        .name("sk")
                        .type(AttributeType.STRING)
                        .build())
                .timeToLiveAttribute("ttl") //time to live
                .removalPolicy(RemovalPolicy.DESTROY) //se for apagado, remover tudo
//                .removalPolicy(RemovalPolicy.RETAIN) //se for apagado, remover tudo
                .build();

        //autoscaling read
/*
        this.productEventTable.autoScaleReadCapacity(EnableScalingProps.builder()
                        .maxCapacity(1)
                        .maxCapacity(3)
                        .build())
                .scaleOnUtilization(UtilizationScalingProps.builder()
                        .targetUtilizationPercent(50)
                        .scaleInCooldown(Duration.seconds(30))
                        .scaleOutCooldown(Duration.seconds(30))
                        .build());
*/

        //autoscaling write
/*
        this.productEventTable.autoScaleWriteCapacity(EnableScalingProps.builder()
                        .maxCapacity(1)
                        .maxCapacity(3)
                        .build())
                .scaleOnUtilization(UtilizationScalingProps.builder()
                        .targetUtilizationPercent(50)
                        .scaleInCooldown(Duration.seconds(30))
                        .scaleOutCooldown(Duration.seconds(30))
                        .build());
*/

    }

    public Table getProductEventTable() {
        return this.productEventTable;
    }
}
