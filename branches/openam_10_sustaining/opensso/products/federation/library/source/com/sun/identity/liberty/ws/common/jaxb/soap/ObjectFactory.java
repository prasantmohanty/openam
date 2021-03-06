//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v1.0.6-b27-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.09.20 at 09:12:12 DU CEST 
//


package com.sun.identity.liberty.ws.common.jaxb.soap;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.sun.identity.liberty.ws.common.jaxb.soap package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
public class ObjectFactory
    extends com.sun.identity.federation.jaxb.entityconfig.impl.runtime.DefaultJAXBContextImpl
{

    private static java.util.HashMap defaultImplementations = new java.util.HashMap(16, 0.75F);
    private static java.util.HashMap rootTagMap = new java.util.HashMap();
    public final static com.sun.identity.federation.jaxb.entityconfig.impl.runtime.GrammarInfo grammarInfo = new com.sun.identity.federation.jaxb.entityconfig.impl.runtime.GrammarInfoImpl(rootTagMap, defaultImplementations, (com.sun.identity.liberty.ws.common.jaxb.soap.ObjectFactory.class));
    public final static java.lang.Class version = (com.sun.identity.liberty.ws.common.jaxb.soap.impl.JAXBVersion.class);

    static {
        defaultImplementations.put((com.sun.identity.liberty.ws.common.jaxb.soap.HeaderType.class), "com.sun.identity.liberty.ws.common.jaxb.soap.impl.HeaderTypeImpl");
        defaultImplementations.put((com.sun.identity.liberty.ws.common.jaxb.soap.FaultElement.class), "com.sun.identity.liberty.ws.common.jaxb.soap.impl.FaultElementImpl");
        defaultImplementations.put((com.sun.identity.liberty.ws.common.jaxb.soap.BodyElement.class), "com.sun.identity.liberty.ws.common.jaxb.soap.impl.BodyElementImpl");
        defaultImplementations.put((com.sun.identity.liberty.ws.common.jaxb.soap.Detail.class), "com.sun.identity.liberty.ws.common.jaxb.soap.impl.DetailImpl");
        defaultImplementations.put((com.sun.identity.liberty.ws.common.jaxb.soap.EnvelopeType.class), "com.sun.identity.liberty.ws.common.jaxb.soap.impl.EnvelopeTypeImpl");
        defaultImplementations.put((com.sun.identity.liberty.ws.common.jaxb.soap.FaultType.class), "com.sun.identity.liberty.ws.common.jaxb.soap.impl.FaultTypeImpl");
        defaultImplementations.put((com.sun.identity.liberty.ws.common.jaxb.soap.EnvelopeElement.class), "com.sun.identity.liberty.ws.common.jaxb.soap.impl.EnvelopeElementImpl");
        defaultImplementations.put((com.sun.identity.liberty.ws.common.jaxb.soap.HeaderElement.class), "com.sun.identity.liberty.ws.common.jaxb.soap.impl.HeaderElementImpl");
        defaultImplementations.put((com.sun.identity.liberty.ws.common.jaxb.soap.BodyType.class), "com.sun.identity.liberty.ws.common.jaxb.soap.impl.BodyTypeImpl");
        rootTagMap.put(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/envelope/", "Fault"), (com.sun.identity.liberty.ws.common.jaxb.soap.FaultElement.class));
        rootTagMap.put(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/envelope/", "Body"), (com.sun.identity.liberty.ws.common.jaxb.soap.BodyElement.class));
        rootTagMap.put(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/envelope/", "Envelope"), (com.sun.identity.liberty.ws.common.jaxb.soap.EnvelopeElement.class));
        rootTagMap.put(new javax.xml.namespace.QName("http://schemas.xmlsoap.org/soap/envelope/", "Header"), (com.sun.identity.liberty.ws.common.jaxb.soap.HeaderElement.class));
    }

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.sun.identity.liberty.ws.common.jaxb.soap
     * 
     */
    public ObjectFactory() {
        super(grammarInfo);
    }

    /**
     * Create an instance of the specified Java content interface.
     * 
     * @param javaContentInterface
     *     the Class object of the javacontent interface to instantiate
     * @return
     *     a new instance
     * @throws JAXBException
     *     if an error occurs
     */
    public java.lang.Object newInstance(java.lang.Class javaContentInterface)
        throws javax.xml.bind.JAXBException
    {
        return super.newInstance(javaContentInterface);
    }

    /**
     * Get the specified property. This method can only be
     * used to get provider specific properties.
     * Attempting to get an undefined property will result
     * in a PropertyException being thrown.
     * 
     * @param name
     *     the name of the property to retrieve
     * @return
     *     the value of the requested property
     * @throws PropertyException
     *     when there is an error retrieving the given property or value
     */
    public java.lang.Object getProperty(java.lang.String name)
        throws javax.xml.bind.PropertyException
    {
        return super.getProperty(name);
    }

    /**
     * Set the specified property. This method can only be
     * used to set provider specific properties.
     * Attempting to set an undefined property will result
     * in a PropertyException being thrown.
     * 
     * @param name
     *     the name of the property to retrieve
     * @param value
     *     the value of the property to be set
     * @throws PropertyException
     *     when there is an error processing the given property or value
     */
    public void setProperty(java.lang.String name, java.lang.Object value)
        throws javax.xml.bind.PropertyException
    {
        super.setProperty(name, value);
    }

    /**
     * Create an instance of HeaderType
     * 
     * @throws JAXBException
     *     if an error occurs
     */
    public com.sun.identity.liberty.ws.common.jaxb.soap.HeaderType createHeaderType()
        throws javax.xml.bind.JAXBException
    {
        return new com.sun.identity.liberty.ws.common.jaxb.soap.impl.HeaderTypeImpl();
    }

    /**
     * Create an instance of FaultElement
     * 
     * @throws JAXBException
     *     if an error occurs
     */
    public com.sun.identity.liberty.ws.common.jaxb.soap.FaultElement createFaultElement()
        throws javax.xml.bind.JAXBException
    {
        return new com.sun.identity.liberty.ws.common.jaxb.soap.impl.FaultElementImpl();
    }

    /**
     * Create an instance of BodyElement
     * 
     * @throws JAXBException
     *     if an error occurs
     */
    public com.sun.identity.liberty.ws.common.jaxb.soap.BodyElement createBodyElement()
        throws javax.xml.bind.JAXBException
    {
        return new com.sun.identity.liberty.ws.common.jaxb.soap.impl.BodyElementImpl();
    }

    /**
     * Create an instance of Detail
     * 
     * @throws JAXBException
     *     if an error occurs
     */
    public com.sun.identity.liberty.ws.common.jaxb.soap.Detail createDetail()
        throws javax.xml.bind.JAXBException
    {
        return new com.sun.identity.liberty.ws.common.jaxb.soap.impl.DetailImpl();
    }

    /**
     * Create an instance of EnvelopeType
     * 
     * @throws JAXBException
     *     if an error occurs
     */
    public com.sun.identity.liberty.ws.common.jaxb.soap.EnvelopeType createEnvelopeType()
        throws javax.xml.bind.JAXBException
    {
        return new com.sun.identity.liberty.ws.common.jaxb.soap.impl.EnvelopeTypeImpl();
    }

    /**
     * Create an instance of FaultType
     * 
     * @throws JAXBException
     *     if an error occurs
     */
    public com.sun.identity.liberty.ws.common.jaxb.soap.FaultType createFaultType()
        throws javax.xml.bind.JAXBException
    {
        return new com.sun.identity.liberty.ws.common.jaxb.soap.impl.FaultTypeImpl();
    }

    /**
     * Create an instance of EnvelopeElement
     * 
     * @throws JAXBException
     *     if an error occurs
     */
    public com.sun.identity.liberty.ws.common.jaxb.soap.EnvelopeElement createEnvelopeElement()
        throws javax.xml.bind.JAXBException
    {
        return new com.sun.identity.liberty.ws.common.jaxb.soap.impl.EnvelopeElementImpl();
    }

    /**
     * Create an instance of HeaderElement
     * 
     * @throws JAXBException
     *     if an error occurs
     */
    public com.sun.identity.liberty.ws.common.jaxb.soap.HeaderElement createHeaderElement()
        throws javax.xml.bind.JAXBException
    {
        return new com.sun.identity.liberty.ws.common.jaxb.soap.impl.HeaderElementImpl();
    }

    /**
     * Create an instance of BodyType
     * 
     * @throws JAXBException
     *     if an error occurs
     */
    public com.sun.identity.liberty.ws.common.jaxb.soap.BodyType createBodyType()
        throws javax.xml.bind.JAXBException
    {
        return new com.sun.identity.liberty.ws.common.jaxb.soap.impl.BodyTypeImpl();
    }

}
