package com.imbilalbutt.stack;


import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

import java.util.stream.Collectors;

public class LocalStack extends Stack {
    // 5. Firstly we will create VPC
//    VPC is top level AWS component where all of our infrasture is going to live
//    VPC also sets up all the networking needed for all of our services and resources to communicate with each other
    private final Vpc vpc;


    public  LocalStack(final App scope, final String id, final StackProps props) {
        super(scope, id, props);
        this.vpc = createVpc();

        DatabaseInstance auth_db_service = createDatabaseInstance("AuthDbService", "auth-db-service");

        DatabaseInstance patient_db_service = createDatabaseInstance("PatientDbService", "patient-db-service");

        CfnHealthCheck auth_db_health_check = createDbHealthCheck(auth_db_service, "AuthDbServiceHealthCheck");

        CfnHealthCheck patient_db_health_check = createDbHealthCheck(patient_db_service, "PatientDbServiceHealthCheck");
    }
//
//// 6.   scope is stack we are currently in ==> this referes to LocalStack class
    private Vpc createVpc() {
        return Vpc.Builder.create(this, "PatientManagementVP")
                .vpcName("PatientManagementVP")
//                it refers to max available zones
                .maxAzs(2) // by saying 2 we are saying it is available at 2 zones
                .build();
    }


// 7. We will create a method that we can resue to create a different databases
    private DatabaseInstance createDatabaseInstance(String id, String dbName) {
        return DatabaseInstance
                .Builder
                .create(this, id)
                .engine(
                        DatabaseInstanceEngine
                                .postgres(PostgresInstanceEngineProps.builder()
                                .version(PostgresEngineVersion.VER_17_2).build())
                        )
                .vpc(this.vpc)
//                instanceType is combination of cpu, computer and storage we want this db to run on
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                .allocatedStorage(20) //
                .credentials(Credentials.fromGeneratedSecret("admin_user"))
                .databaseName(dbName)
                .removalPolicy(RemovalPolicy.DESTROY) // everytime we destroy stack we also want to remove db storage
                .build();
    }

//    8. health check
    private CfnHealthCheck createDbHealthCheck(DatabaseInstance db, String id) {
        return CfnHealthCheck
                .Builder
                .create(this, id)
                .healthCheckConfig(
                        CfnHealthCheck
                                .HealthCheckConfigProperty
                                .builder()
                                .type("TCP")
                                .port(Token.asNumber(db.getDbInstanceEndpointPort()))
                                .ipAddress(db.getDbInstanceEndpointAddress())
                                .requestInterval(30) //check health end point after 30 seconds
                                .failureThreshold(3)
                                .build()
                )
                .build();
    }

//    9. Create a method that will create an MSK Kafka cluster for us
    private CfnCluster createMSKCluster(){
        return CfnCluster
                .Builder
                .create(this, "MSKCluster")
                .clusterName("kafka-cluster")
                .kafkaVersion("2.8.0")
                .numberOfBrokerNodes(1)
                .brokerNodeGroupInfo(
                        CfnCluster
                        .BrokerNodeGroupInfoProperty
                        .builder()
                        .instanceType("kafka.m5.xlarge")
                                // this is how we connect out Kafka to VPC connection
                        .clientSubnets(vpc.getPrivateSubnets().stream().map(ISubnet::getSubnetId).collect(Collectors.toList()))
                        .build()
                )
                .build();
    }


// 4.   initialization only, do nothing
    public static void main(final String[] args) {
//  1.     we are creating new CDK app and we are defining where we want output to be
        App app = new App(AppProps.builder().outdir("./cdk.out").build());
//  2.      we are defning additional property "props" that we want to apply to our stack
//        Synthesier is used to convert our code into cloud formation template
//        BootstraplessSynthesizer is telling CDK code to skip the initial bootstraping of the CDK env as we do not need
//        for LocalStack
        StackProps props = StackProps.builder()
                .synthesizer(new BootstraplessSynthesizer())
                .build();

//  3.    Lastly we need to link our "LocalStack class" to this "App app" so that CDK knows to build our stack anytime we run this java app
        new LocalStack(app, "localstack", props);
        app.synth();

        System.out.println("App synthesizing in progress...");

    }

}
