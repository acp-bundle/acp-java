# Admin control panel based on jetty

## compile

This project uses maven. Use `mvn clean install` to get an executable jar file. Then place `target/lib` alongside the `acp-java-<version>.jar`.

## starting the acp

```bash
java -jar acp-java.jar
```
generates a template `config.json`. Edit the config file to fit your needs. THEN run
```bash
java -jar acp-java.jar generate-tables config.json
```
to generate the initial tables. You may notice that all text fields are of type `VARCHAR(8192)`. You may shrink them down manually if you want. After that run
```bash
java -jar acp-java.jar add-superadmin config.json <Username> <Password>
```
to add a new superuser with given username and password. After that you can start the ACP with:
```bash
java -jar acp-java.jar config.json
```
and connect with your webbrowser to the correct port.

## suggested setup
The ACP does not support any https by itself. I highly suggest to put it behind an apache or nginx proxy. Make sure that the proxy sets the `X-Real-IP` header correctly to make use of IP based access. Those headers are only obeyed when binding the server to `127.0.0.1`.

## disable console logging
Add `"disable_cout" : "true"` into the `config.json`.
