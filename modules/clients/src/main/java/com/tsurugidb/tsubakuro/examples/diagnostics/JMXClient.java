package com.tsurugidb.tsubakuro.examples.diagnostics;

import java.io.IOException;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.tsurugidb.tsubakuro.diagnostic.common.SessionInfoMBean;

public class JMXClient {
	public static void main(String[] args) {
		try {
			JMXConnector jmxc = JMXConnectorFactory.connect(new JMXServiceURL(
					"service:jmx:rmi:///jndi/rmi://:9999/jmxrmi"));
			MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
			ObjectName mbeanName = new ObjectName("com.tsurugidb.tsubakuro.diagnostic.common:type=SessionInfo");
			SessionInfoMBean mbeanProxy = JMX.newMBeanProxy(mbsc, mbeanName, SessionInfoMBean.class, true);

			String sessionInfo = mbeanProxy.getSessionInfo();
			System.out.println(sessionInfo);
	} catch (IOException e) {
			e.printStackTrace();
		} catch (MalformedObjectNameException e) {
			e.printStackTrace();
		}
	}
}
