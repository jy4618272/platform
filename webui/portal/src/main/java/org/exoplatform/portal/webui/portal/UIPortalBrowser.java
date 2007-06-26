package org.exoplatform.portal.webui.portal;

import java.util.Iterator;
import java.util.List;

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.DataStorage;
import org.exoplatform.portal.config.Query;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.portal.webui.workspace.UIMaskWorkspace;
import org.exoplatform.portal.webui.workspace.UIPortalApplication;
import org.exoplatform.portal.webui.workspace.UIWorkspace;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIGrid;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(
  template = "app:/groovy/portal/webui/portal/UIPortalBrowser.gtmpl",
  events = { 
      @EventConfig(listeners = UIPortalBrowser.AddNewPortalActionListener.class),
      @EventConfig(listeners = UIPortalBrowser.DeletePortalActionListener.class, confirm = "UIPortalBrowser.deletePortal")
  }
)
public class UIPortalBrowser extends UIContainer {

  public static String[] BEAN_FIELD = {"creator", "name", "skin", "factoryId"} ;  
  public static String[] SELECT_ACTIONS = {"DeletePortal"} ; 
  
  public UIPortalBrowser() throws Exception {
    setId("UIPortalBrowser");
    
    UIGrid uiGrid = addChild(UIGrid.class, null, null) ;
    uiGrid.configure("name", BEAN_FIELD, SELECT_ACTIONS) ;
    addChild(uiGrid.getUIPageIterator()) ;
    uiGrid.getUIPageIterator().setRendered(false) ;
    
    loadPortalConfigs();
  }

  public void loadPortalConfigs() throws Exception {    
    DataStorage service = getApplicationComponent(DataStorage.class) ;
    UserACL userACL = getApplicationComponent(UserACL.class) ;
    String accessUser = Util.getPortalRequestContext().getRemoteUser() ;
    Query<PortalConfig> query = new Query<PortalConfig>(null, null, null, PortalConfig.class) ;
    PageList pageList = service.find(query) ;
    pageList.setPageSize(10) ;
    int i = 1 ;
    while(i <= pageList.getAvailablePage()) {
      List<?> list = pageList.getPage(i) ;
      Iterator<?> itr = list.iterator() ;
      while(itr.hasNext()) {
        PortalConfig portalConfig = (PortalConfig)itr.next() ;
        String creator = portalConfig.getCreator();
        String [] accessPerm = portalConfig.getAccessPermissions();
        if(!userACL.hasViewPermission(creator, accessUser, accessPerm)) itr.remove() ;
      }
      i++ ;
    }
    UIGrid uiGrid = findFirstComponentOfType(UIGrid.class) ;
    uiGrid.setUseAjax(false);
    uiGrid.getUIPageIterator().setPageList(pageList);
  } 

  static public class DeletePortalActionListener extends EventListener<UIPortalBrowser> {
    public void execute(Event<UIPortalBrowser> event) throws Exception {
      String portalName = event.getRequestContext().getRequestParameter(OBJECTID) ;
      UserPortalConfigService service = event.getSource().getApplicationComponent(UserPortalConfigService.class);
      WebuiRequestContext webuiContext = event.getRequestContext();
      UserPortalConfig config = service.getUserPortalConfig(portalName, webuiContext.getRemoteUser());
      if(config != null && config.getPortalConfig().isModifiable()) {
        service.removeUserPortalConfig(portalName);
      }
      event.getSource().loadPortalConfigs();
      UIPortalApplication uiPortalApp = event.getSource().getAncestorOfType(UIPortalApplication.class);
      UIWorkspace uiWorkingWS = uiPortalApp.findComponentById(UIPortalApplication.UI_WORKING_WS_ID);    
      event.getRequestContext().addUIComponentToUpdateByAjax(uiWorkingWS) ;
    }
  }
  
  static public class AddNewPortalActionListener extends EventListener<UIPortalBrowser> {
    public void execute(Event<UIPortalBrowser> event) throws Exception {
      PortalRequestContext prContext = Util.getPortalRequestContext();
      UIPortalApplication uiApp = event.getSource().getAncestorOfType(UIPortalApplication.class);      
      UIMaskWorkspace uiMaskWS = uiApp.getChildById(UIPortalApplication.UI_MASK_WS_ID) ;
      UIPortalForm uiNewPortal = uiMaskWS.createUIComponent(UIPortalForm.class, "CreatePortal", "UIPortalForm");
      uiMaskWS.setUIComponent(uiNewPortal);
      uiMaskWS.setShow(true);
      prContext.addUIComponentToUpdateByAjax(uiMaskWS);
    }
  }
}
