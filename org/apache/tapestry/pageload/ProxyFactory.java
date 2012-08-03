package org.apache.tapestry.pageload;

import java.lang.reflect.Method;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.apache.tapestry.IPage;

public class ProxyFactory  {
	static class MethodInterceptorImpl implements MethodInterceptor{
		@Override
		public Object intercept(Object obj, Method method, Object[] args,
				MethodProxy proxy) throws Throwable {
			System.out.println("do something different");
			return proxy.invokeSuper(obj, args);  
		}
	}
	public static IPage getPageProxy(Class clazz) throws Exception{
		Enhancer enhancer = new  Enhancer();  
	    enhancer.setSuperclass(clazz);  
	    enhancer.setCallback(new  MethodInterceptorImpl());  
	    IPage page = (IPage) enhancer.create();  
	    return page;
	}
	
	

}
