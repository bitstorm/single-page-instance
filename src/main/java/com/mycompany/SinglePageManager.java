package com.mycompany;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.wicket.IPageFactory;
import org.apache.wicket.MetaDataKey;
import org.apache.wicket.Page;
import org.apache.wicket.Session;
import org.apache.wicket.core.request.handler.IPageClassRequestHandler;
import org.apache.wicket.page.CouldNotLockPageException;
import org.apache.wicket.page.IManageablePage;
import org.apache.wicket.page.IPageManager;
import org.apache.wicket.page.IPageManagerContext;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.cycle.PageRequestHandlerTracker;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.lang.Args;

/**
 *
 */
public class SinglePageManager implements IPageManager, IPageFactory
{
	private final IPageManager manager;
	private final IPageFactory factory;
	
	public static MetaDataKey<ConcurrentHashMap<Class<? extends IManageablePage>, Integer>> CLASS_PAGE_ID_MAP_KEY = 
		new MetaDataKey<ConcurrentHashMap<Class<? extends IManageablePage>, Integer>> (){};
	
	public SinglePageManager(IPageManager manager, IPageFactory factory) {
		this.manager = Args.notNull(manager, "manager");
		this.factory = Args.notNull(factory, "factory");
	}

	@Override
	public IPageManagerContext getContext()
	{
		return manager.getContext();
	}

	@Override
	public IManageablePage getPage(int id) throws CouldNotLockPageException
	{
		RequestCycle cycle = RequestCycle.get();
		IRequestHandler requestHandler = PageRequestHandlerTracker.getFirstHandler(cycle);
		if (requestHandler instanceof IPageClassRequestHandler)
		{
			IPageClassRequestHandler pageClassRequestHandler = (IPageClassRequestHandler) requestHandler;
			Class<? extends IRequestablePage> pageClass = pageClassRequestHandler.getPageClass();
			Integer pageId = getPageId(pageClass);
			
			if (pageId != null)
			{
				return manager.getPage(pageId);
			}
		}

		return manager.getPage(id);
	}

	private Integer getPageId(Class<? extends IRequestablePage> pageClass)
	{
		return getSessionPageMap().get(pageClass);
	}

	@Override
	public void touchPage(IManageablePage page) throws CouldNotLockPageException
	{
		putIfAbsent(page.getClass(), page.getPageId());
		manager.touchPage(page);
	}

	private void putIfAbsent(Class<? extends IManageablePage> clazz, int pageId)
	{
		getSessionPageMap().putIfAbsent(clazz, pageId);
	}

	private ConcurrentHashMap<Class<? extends IManageablePage>, Integer> getSessionPageMap()
	{
		return Session.get().getMetaData(CLASS_PAGE_ID_MAP_KEY);
	}

	@Override
	public boolean supportsVersioning()
	{
		return false;
	}

	@Override
	public void commitRequest()
	{
		manager.commitRequest();
	}

	@Override
	public void newSessionCreated()
	{
		manager.newSessionCreated();
		
		ConcurrentHashMap<Class<? extends IManageablePage>, Integer> pageMap = new ConcurrentHashMap<>();
		Session.get().setMetaData(CLASS_PAGE_ID_MAP_KEY, pageMap);
	}

	@Override
	public void clear()
	{
		manager.clear();
	}

	@Override
	public void destroy()
	{
		manager.destroy();
	}

	@Override
	public <C extends IRequestablePage> C newPage(Class<C> pageClass)
	{
		return newPage(pageClass, new PageParameters());
	}

	@Override
	public <C extends IRequestablePage> C newPage(Class<C> pageClass, PageParameters parameters)
	{
		Page page = null;
		Integer pageId = getPageId(pageClass);
		if (pageId != null)
		{
			page = (Page) manager.getPage(pageId);
		}

		if (page != null)
		{
			page.getPageParameters().overwriteWith(parameters);
			return (C) page;
		}
		else
		{
			return factory.newPage(pageClass, parameters);
		}
	}

	@Override
	public <C extends IRequestablePage> boolean isBookmarkable(Class<C> pageClass)
	{
		return factory.isBookmarkable(pageClass);
	}
}
