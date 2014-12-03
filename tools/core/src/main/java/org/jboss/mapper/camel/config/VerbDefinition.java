//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.11.26 at 02:37:57 PM EST 
//


package org.jboss.mapper.camel.config;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for verbDefinition complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="verbDefinition">
 *   &lt;complexContent>
 *     &lt;extension base="{http://camel.apache.org/schema/spring}optionalIdentifiedDefinition">
 *       &lt;sequence>
 *         &lt;choice minOccurs="0">
 *           &lt;element ref="{http://camel.apache.org/schema/spring}to"/>
 *           &lt;element ref="{http://camel.apache.org/schema/spring}route"/>
 *         &lt;/choice>
 *       &lt;/sequence>
 *       &lt;attribute name="method" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="uri" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="consumes" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="produces" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="bindingMode" type="{http://camel.apache.org/schema/spring}restBindingMode" />
 *       &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="outType" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "verbDefinition", propOrder = {
    "to",
    "route"
})
@XmlSeeAlso({
    PutVerbDefinition.class,
    DeleteVerbDefinition.class,
    PostVerbDefinition.class,
    HeadVerbDefinition.class,
    GetVerbDefinition.class
})
public class VerbDefinition
    extends OptionalIdentifiedDefinition
{

    protected ToDefinition to;
    protected RouteDefinition route;
    @XmlAttribute(name = "method")
    protected String method;
    @XmlAttribute(name = "uri")
    protected String uri;
    @XmlAttribute(name = "consumes")
    protected String consumes;
    @XmlAttribute(name = "produces")
    protected String produces;
    @XmlAttribute(name = "bindingMode")
    protected RestBindingMode bindingMode;
    @XmlAttribute(name = "type")
    protected String type;
    @XmlAttribute(name = "outType")
    protected String outType;

    /**
     * Gets the value of the to property.
     * 
     * @return
     *     possible object is
     *     {@link ToDefinition }
     *     
     */
    public ToDefinition getTo() {
        return to;
    }

    /**
     * Sets the value of the to property.
     * 
     * @param value
     *     allowed object is
     *     {@link ToDefinition }
     *     
     */
    public void setTo(ToDefinition value) {
        this.to = value;
    }

    /**
     * Gets the value of the route property.
     * 
     * @return
     *     possible object is
     *     {@link RouteDefinition }
     *     
     */
    public RouteDefinition getRoute() {
        return route;
    }

    /**
     * Sets the value of the route property.
     * 
     * @param value
     *     allowed object is
     *     {@link RouteDefinition }
     *     
     */
    public void setRoute(RouteDefinition value) {
        this.route = value;
    }

    /**
     * Gets the value of the method property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets the value of the method property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMethod(String value) {
        this.method = value;
    }

    /**
     * Gets the value of the uri property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getUri() {
        return uri;
    }

    /**
     * Sets the value of the uri property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setUri(String value) {
        this.uri = value;
    }

    /**
     * Gets the value of the consumes property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getConsumes() {
        return consumes;
    }

    /**
     * Sets the value of the consumes property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setConsumes(String value) {
        this.consumes = value;
    }

    /**
     * Gets the value of the produces property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProduces() {
        return produces;
    }

    /**
     * Sets the value of the produces property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProduces(String value) {
        this.produces = value;
    }

    /**
     * Gets the value of the bindingMode property.
     * 
     * @return
     *     possible object is
     *     {@link RestBindingMode }
     *     
     */
    public RestBindingMode getBindingMode() {
        return bindingMode;
    }

    /**
     * Sets the value of the bindingMode property.
     * 
     * @param value
     *     allowed object is
     *     {@link RestBindingMode }
     *     
     */
    public void setBindingMode(RestBindingMode value) {
        this.bindingMode = value;
    }

    /**
     * Gets the value of the type property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setType(String value) {
        this.type = value;
    }

    /**
     * Gets the value of the outType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOutType() {
        return outType;
    }

    /**
     * Sets the value of the outType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOutType(String value) {
        this.outType = value;
    }

}