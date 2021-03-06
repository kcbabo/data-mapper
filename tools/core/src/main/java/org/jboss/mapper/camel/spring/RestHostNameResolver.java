//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.11.26 at 02:37:57 PM EST 
//


package org.jboss.mapper.camel.spring;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for restHostNameResolver.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="restHostNameResolver">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="localHostName"/>
 *     &lt;enumeration value="localIp"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "restHostNameResolver")
@XmlEnum
public enum RestHostNameResolver {

    @XmlEnumValue("localHostName")
    LOCAL_HOST_NAME("localHostName"),
    @XmlEnumValue("localIp")
    LOCAL_IP("localIp");
    private final String value;

    RestHostNameResolver(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static RestHostNameResolver fromValue(String v) {
        for (RestHostNameResolver c: RestHostNameResolver.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
