#!/bin/bash

javac -cp src/jgrapht-core-1.0.0.jar src/FraudUserStat.java src/User.java
java -cp src/jgrapht-core-1.0.0.jar:paymo_input:paymo_output:src FraudUserStat paymo_input/batch_payment.txt paymo_input/stream_payment.txt 1
