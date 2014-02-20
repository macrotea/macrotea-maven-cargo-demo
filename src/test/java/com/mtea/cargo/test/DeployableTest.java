package com.mtea.cargo.test;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;

import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.deployer.DeployableMonitorListener;
import org.codehaus.cargo.container.deployer.Deployer;
import org.codehaus.cargo.container.deployer.URLDeployableMonitor;
import org.codehaus.cargo.container.tomcat.Tomcat6xInstalledLocalContainer;
import org.codehaus.cargo.container.tomcat.Tomcat6xStandaloneLocalConfiguration;
import org.codehaus.cargo.container.tomcat.TomcatCopyingInstalledLocalDeployer;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.ConfigurationFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;
import org.codehaus.cargo.generic.deployable.DefaultDeployableFactory;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeployableTest {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	private String warPath = "target/cargo-demo.war";
	private String warBakPath = "target/cargo-demo-bak.war";
	private String tomcatConfigProfilePath = "target/tomcat6";
	private String tomcatHome = "C:/_Quick/Develop/Server/apache-tomcat-6.0.37-windows-x64";

	//@Test
	public void test_container_start_stop() throws InterruptedException {
		Deployable war = new WAR(warPath);

		// 需要把本地tomcat的配置复制到此路径下作为测试的Profile
		LocalConfiguration configuration = new Tomcat6xStandaloneLocalConfiguration(tomcatConfigProfilePath);
		configuration.addDeployable(war);

		InstalledLocalContainer container = new Tomcat6xInstalledLocalContainer(configuration);
		container.setHome(tomcatHome);

		// 启动阻塞
		container.start();

		logger.debug("container.getOutput() : " + container.getOutput());
		logger.debug("container.getState().isStarted() : " + container.getState().isStarted());

		assertTrue(container.getState().isStarted());

		Thread.sleep(5000);

		// 停止阻塞
		container.stop();

		assertTrue(container.getState().isStopped());
		logger.debug("container.getState().isStopped() : " + container.getState().isStopped());
	}

	/**
	 * 通用写法
	 * 
	 * @author macrotea@qq.com
	 * @throws InterruptedException
	 */
	//@Test
	public void test_container_start_stop2() throws InterruptedException {
		String containerType = "tomcat6x";

		Deployable war = new DefaultDeployableFactory().createDeployable(containerType, warPath, DeployableType.WAR);

		ConfigurationFactory configurationFactory = new DefaultConfigurationFactory();
		LocalConfiguration configuration = (LocalConfiguration) configurationFactory.createConfiguration(containerType, ContainerType.INSTALLED, ConfigurationType.STANDALONE);
		configuration.addDeployable(war);

		InstalledLocalContainer container = (InstalledLocalContainer) new DefaultContainerFactory().createContainer(containerType, ContainerType.INSTALLED, configuration);
		container.setHome(tomcatHome);

		// 启动阻塞
		container.start();

		logger.debug(containerType + " container.getOutput() : " + container.getOutput());
		logger.debug(containerType + " container.getState().isStarted() : " + container.getState().isStarted());

		assertTrue(container.getState().isStarted());

		Thread.sleep(5000);

		// 停止阻塞
		container.stop();

		assertTrue(container.getState().isStopped());
		logger.debug(containerType + " container.getState().isStopped() : " + container.getState().isStopped());
	}

	/**
	 * 热部署
	 * 
	 * @author macrotea@qq.com
	 * @throws InterruptedException
	 * @throws MalformedURLException
	 */
	@Test
	public void test_hot_deploy() throws InterruptedException, MalformedURLException {
		final InstalledLocalContainer container = new Tomcat6xInstalledLocalContainer(new Tomcat6xStandaloneLocalConfiguration(tomcatConfigProfilePath));
		container.setHome(tomcatHome);

		container.start();

		assertTrue(container.getState().isStarted());

		Deployable warBak = new WAR(warBakPath);
		Deployer deployer = new TomcatCopyingInstalledLocalDeployer(container);
		deployer.deploy(warBak);

		String url = "http://localhost:8080/cargo-demo-bak/index.jsp";
		logger.debug("url: " + url);
		final URLDeployableMonitor monitor = new URLDeployableMonitor(new URL(url));
		monitor.registerListener(new DeployableMonitorListener() {
			
			public void undeployed() {
				logger.debug("undeployed");
				//logger.debug("monitor - container.getState().isStopped(): " + container.getState().isStopped());
				//logger.debug("monitor.getDeployableName(): " + monitor.getDeployableName());
			}
			
			public void deployed() {
				logger.debug("deployed");
				container.stop();
				assertTrue(container.getState().isStopped());
			}
		});
		
		deployer.deploy(warBak, monitor);
		//for DeployableMonitorListener
		Thread.sleep(20000);
	}

	// public static void main(String[] args) throws InterruptedException {
	// new DeployableTest().test_container_start_stop();
	// }
}
