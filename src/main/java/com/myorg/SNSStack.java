package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.events.targets.SnsTopic;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.subscriptions.EmailSubscription;
import software.constructs.Construct;

public class SNSStack extends Stack {

    private SnsTopic productEventsTopic;

    public SNSStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public SNSStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.productEventsTopic = SnsTopic.Builder
                .create(Topic.Builder.create(this, "PRODUCTS-EVENTS-TOPIC-01")
                        .topicName("PRODUCT-EVENTS")
                        .build())
                .build();

        this.productEventsTopic.getTopic()
                .addSubscription(EmailSubscription.Builder.create("renato@likwi.com.br").json(Boolean.TRUE).build());

    }

    public SnsTopic getProductEventsTopic() {
        return productEventsTopic;
    }
}
