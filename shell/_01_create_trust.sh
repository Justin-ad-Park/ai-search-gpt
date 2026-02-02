JH=$(/usr/libexec/java_home -v 21)
"$JH/bin/keytool" -list -v -storetype PKCS12 -keystore .gradle/djl-truststore.p12 -storepass changeit | sed -n '1,240p'