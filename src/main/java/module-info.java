module io.github.udonabe.donabe {
    requires ch.qos.logback.classic;
    requires ch.qos.logback.core;
    requires info.picocli;
    requires org.apache.commons.lang3;
    requires org.slf4j;
    requires java.desktop;

    opens io.github.udonabe.donabe;
    opens io.github.udonabe.donabe.error;
}