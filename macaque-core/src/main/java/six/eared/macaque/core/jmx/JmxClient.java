package six.eared.macaque.core.jmx;

import six.eared.macaque.core.exception.JmxConnectException;
import six.eared.macaque.mbean.MBean;
import six.eared.macaque.mbean.MBeanObjectName;
import six.eared.macaque.mbean.rmi.EmptyRmiData;
import six.eared.macaque.mbean.rmi.RmiData;

import javax.management.JMX;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;

public class JmxClient {

    private String host;

    private Integer port;

    private JMXConnector connector;

    private MBean<EmptyRmiData> hearbeatMBean;

    public JmxClient(String host, Integer port) {
        this.host = host;
        this.port = port;
    }

    public void connect() {
        try {
            String url = String.format("service:jmx:rmi:///jndi/rmi://%s:%d/macaque", host, port);
            JMXServiceURL serviceURL = new JMXServiceURL(url);
            this.connector = JMXConnectorFactory.connect(serviceURL);
            this.hearbeatMBean = getMBean(MBeanObjectName.HEART_BEAT_MBEAN);
        } catch (Exception e) {
            throw new JmxConnectException(host + ":" + port, e);
        }
    }

    public void disconnect() throws IOException {
        try {
            if (this.connector != null) {
                this.connector.close();
                this.connector = null;
            }
            if (this.hearbeatMBean != null) {
                this.hearbeatMBean = null;
            }
        } catch (IOException e) {
            throw e;
        }
    }

    public boolean isConnect() {
        try {
            return connector != null
                    && this.hearbeatMBean != null
                    && this.hearbeatMBean.process(new EmptyRmiData()).isSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    public <T extends RmiData> MBean<T> getMBean(String objectName) throws Exception {
        return JMX.newMBeanProxy(this.connector.getMBeanServerConnection(),
                new ObjectName(objectName), MBean.class);
    }
}
