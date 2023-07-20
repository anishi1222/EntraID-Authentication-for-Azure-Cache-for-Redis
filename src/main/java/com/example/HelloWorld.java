package com.example;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisStringCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.protocol.ProtocolVersion;
import io.micronaut.configuration.lettuce.DefaultRedisConfiguration;
import io.micronaut.http.*;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Controller("/hello")
public class HelloWorld {

    private final String errorMessage ="""
            {
                "error":"%s"
            }""";

    @Inject
    DefaultRedisConfiguration defaultRedisConfiguration;

    @Get()
    @Produces(MediaType.APPLICATION_JSON)
    public Object getGreeting(@QueryValue("name") Optional<String> name) {

        if(name.isEmpty()) {
            return HttpResponseFactory.INSTANCE
                    .status(HttpStatus.BAD_REQUEST)
                    .body(String.format(errorMessage, "Query parameter (name) is mandatory."));
        }

        try (RedisClient redisClient = this.getRedisClient()) {
            StatefulRedisConnection<String, String> connection = redisClient.connect(StringCodec.UTF8);
            RedisStringCommands<String, String> sync = connection.sync();
            List<KeyValue<String, String>> keyValueArrayList = sync.mget(name.get());
            if(keyValueArrayList.isEmpty()) {
                return HttpResponseFactory.INSTANCE
                        .status(HttpStatus.NOT_FOUND)
                        .body(String.format(errorMessage,
                                String.format("No data related with specified key(%s) is not found.", name.get())));
            }
            ArrayList<Greeting> greetingArrayList = keyValueArrayList.stream()
                    .map(keyValue -> new Greeting(keyValue.getKey(), keyValue.getValue()))
                    .collect(Collectors.toCollection(ArrayList::new));
            connection.close();
            return greetingArrayList;
        }
        catch (Exception e) {
            e.printStackTrace();
            return HttpResponseFactory.INSTANCE
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(String.format(errorMessage, e.getMessage()));
        }
    }

    @Post()
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Object postGreeting(@Body Greeting _greeting) {

        if(_greeting == null) {
            return HttpResponseFactory.INSTANCE
                    .status(HttpStatus.BAD_REQUEST)
                    .body(String.format(errorMessage, "No body message in HTTP Request."));
        }

        try (RedisClient redisClient = this.getRedisClient()) {
            StatefulRedisConnection<String, String> connection = redisClient.connect(StringCodec.UTF8);
            RedisStringCommands<String, String> sync = connection.sync();
            System.out.println(sync.set(_greeting.getName(), _greeting.getMessage()));
            connection.close();
        }
        catch (Exception e) {
            e.printStackTrace();
            return HttpResponseFactory.INSTANCE
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(String.format(errorMessage, e.getMessage()));
        }
        return _greeting;
    }

    private RedisClient getRedisClient() {

        DefaultAzureCredential defaultAzureCredential = new DefaultAzureCredentialBuilder().build();
        String userName = System.getenv("REDIS_USERNAME");

        RedisURI redisURI = RedisURI.Builder.redis(defaultRedisConfiguration.getHost())
                .withPort(defaultRedisConfiguration.getPort())
                .withSsl(defaultRedisConfiguration.isSsl())
                .withClientName(defaultRedisConfiguration.getClientName())
                .withAuthentication(RedisCredentialsProvider.from(() -> new AzureRedisCredentials(userName, defaultAzureCredential)))
                .build();

        RedisClient redisClient = RedisClient.create(redisURI);
        redisClient.setOptions(ClientOptions.builder()
                .socketOptions(SocketOptions.builder()
                        .keepAlive(true)
                        .build())
                .protocolVersion(ProtocolVersion.RESP2)
                .build());
        return redisClient;
    }
}
