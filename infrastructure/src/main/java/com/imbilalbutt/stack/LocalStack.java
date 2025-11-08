package com.imbilalbutt.stack;


import software.amazon.awscdk.services.ec2.Protocol;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.*;
import software.amazon.awscdk.services.*;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.amazon.awscdk.services.msk.CfnCluster;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.route53.CfnHealthCheck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LocalStack extends Stack {
    // 5. Firstly we will create VPC
//    VPC is top level AWS component where all of our infrasture is going to live
//    VPC also sets up all the networking needed for all of our services and resources to communicate with each other
    private final Vpc vpc;
    private final Cluster ecsCluster;


    public  LocalStack(final App scope, final String id, final StackProps props) {
        super(scope, id, props);
        this.vpc = createVpc();

        DatabaseInstance auth_db_service = createDatabaseInstance("AuthDbService", "auth-db-service");

        DatabaseInstance patient_db_service = createDatabaseInstance("PatientDbService", "patient-db-service");

        CfnHealthCheck auth_db_health_check = createDbHealthCheck(auth_db_service, "AuthDbServiceHealthCheck");

        CfnHealthCheck patient_db_health_check = createDbHealthCheck(patient_db_service, "PatientDbServiceHealthCheck");

        CfnCluster mskCluster = createMSKCluster();

        this.ecsCluster = createEcsCluster();

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
//    we create Kafka cluster using MSK
    private CfnCluster createMSKCluster(){
        return CfnCluster
                .Builder
                .create(this, "MSKCluster")
                .clusterName("kafka-cluster")
                .kafkaVersion("2.8.0")
                .numberOfBrokerNodes(1) // number of brokers to add in cluster
                .brokerNodeGroupInfo(
                        CfnCluster
                        .BrokerNodeGroupInfoProperty
                        .builder()
                        .instanceType("kafka.m5.xlarge")
                                // this is how we connect out Kafka to VPC connection
                        .clientSubnets(vpc.getPrivateSubnets().stream().map(ISubnet::getSubnetId).collect(Collectors.toList()))
//                        this defines how are broker gets distributed across different availability zones
                        .brokerAzDistribution("DEFAULT")
                        .build()
                )
                .build();
    }

//    11. For example, when we create Auth-service, other services will be able to find Auth-service
//    by saying auth-service.<cloud_map_name_space> --> auth-service.patient-management.local
//    This is convenient method we do not need to know IPs and internal addresses of ECS
//    we should only know the namespace and actual microservice.
    private Cluster createEcsCluster(){
        return Cluster.Builder.create(this, "PatientManagementCluster")
                .vpc(this.vpc)
//                10. defaultCloudMapNamespace = sets up a Cloud Map Namespace called patient-management.local for service
//                discovery in AWS-ECS allowing microservices to find and communicate with each using this domain
                .defaultCloudMapNamespace(CloudMapNamespaceOptions.builder().name("patient-management.local").build())
                .build();
    }

//    12a. Next we are going to create our first ECS service
//    An ECS service is used to manage an ECS task in terms of load-balancers, managing scaling,
//    the number of resources that a task need and it will also handle fail task by starting a new one
//    An ECS service is going to run a task
//    and a Task is an actual thing going to run Container

//    12b. Fargate is a type of ECS service. It makes it easy to start, stop and scale ECS tasks that run different containers
//    ECS task is going to take in imageName and it is going to build a container from imageName and start container
//    list of ports that we want our Containers to expose
//    Database instance : so if a service that we are about to creates needs to connect to database, we will handle here
//    Each of our service needs environment-variables to run ....
//    these env-variables are gets passed in at a point when container is run ....
//    Since the Fargate service is going to handle this when it creates a task ....
//    and since the ECS Tasks going to handle this when it creates container ....
//    so we need to tell ECS Task what env-variables to use.
    private FargateService createFargateService(String id, String imageName, List<Integer> ports,
                                                DatabaseInstance db, Map<String, String> additionalEnvVars) {
//        13. In order to create Tasks, we first need to create ECS Task definition
//        Task definition = is a blue print for a container, used to define things like cpu, memory, envVars
        FargateTaskDefinition taskDefinition =
                FargateTaskDefinition.Builder.create(this, id + "Task")
                        .cpu(256)
                        .memoryLimitMiB(512)
                        .build();

//        14. Next, we can define actual Container that we want this Task to start
        ContainerDefinitionOptions.Builder containerOptions =
                ContainerDefinitionOptions.builder()
                        .image(ContainerImage.fromRegistry(imageName))
                        .portMappings(ports.stream()
                                .map(port -> PortMapping.builder()
                                        .containerPort(port)
                                        .hostPort(port)
                                        .protocol(Protocol.TCP)
                                        .build())
                                .toList())
                        .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                .logGroup(LogGroup.Builder.create(this, id + "LogGroup")
                                        .logGroupName("/ecs/" + imageName)
                                        .removalPolicy(RemovalPolicy.DESTROY)
                                        .retention(RetentionDays.ONE_DAY)
                                        .build())
                                .streamPrefix(imageName)
                                .build()));

        Map<String, String> envVars = new HashMap<>();
        envVars.put("SPRING_KAFKA_BOOTSTRAP_SERVERS", "localhost.localstack.cloud:4510, localhost.localstack.cloud:4511, localhost.localstack.cloud:4512");

        if(additionalEnvVars != null){
            envVars.putAll(additionalEnvVars);
        }

        if(db != null){
            envVars.put("SPRING_DATASOURCE_URL", "jdbc:postgresql://%s:%s/%s-db".formatted(
                    db.getDbInstanceEndpointAddress(),
                    db.getDbInstanceEndpointPort(),
                    imageName
            ));
            envVars.put("SPRING_DATASOURCE_USERNAME", "admin_user");
            envVars.put("SPRING_DATASOURCE_PASSWORD",
                    db.getSecret().secretValueFromJson("password").toString());
            envVars.put("SPRING_JPA_HIBERNATE_DDL_AUTO", "update");
            envVars.put("SPRING_SQL_INIT_MODE", "always");
            envVars.put("SPRING_DATASOURCE_HIKARI_INITIALIZATION_FAIL_TIMEOUT", "60000");
        }

        containerOptions.environment(envVars);
        taskDefinition.addContainer(imageName + "Container", containerOptions.build());

        return FargateService.Builder.create(this, id)
                .cluster(ecsCluster)
                .taskDefinition(taskDefinition)
                .assignPublicIp(false)
                .serviceName(imageName)
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
