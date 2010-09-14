package org.gatein.example.verticalMenu;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.ResourceBundle;

import javax.portlet.GenericPortlet;
import javax.portlet.MimeResponse;
import javax.portlet.PortletException;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.config.model.PageNavigation;
import org.exoplatform.portal.config.model.PageNode;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.resources.ResourceBundleManager;
import org.exoplatform.webui.organization.OrganizationUtils;
import org.w3c.dom.Element;

public class VerticalMenuPortlet extends GenericPortlet {

	// Various services
	private UserPortalConfigService userPortalConfigService = null;
	private ResourceBundleManager resourceBundleManager = null;

	private String portalURI = null;

	@Override
	public void init() throws PortletException {
		// Retrieving the current Portal container to access its services
		PortalContainer container = PortalContainer.getInstance();

		// Retrieving the service in charge of handling navigation
		userPortalConfigService = (UserPortalConfigService) container
				.getComponentInstanceOfType(UserPortalConfigService.class);

		// Retrieving the service in charge of translation
		resourceBundleManager = (ResourceBundleManager) container
				.getComponentInstanceOfType(ResourceBundleManager.class);
	}

	@Override
	public void doView(RenderRequest request, RenderResponse response)
			throws IOException {
		Writer writer = response.getWriter();

		try {
			PortalRequestContext pcontext = Util.getPortalRequestContext();

			// Retrieve the navigation nodes for the portal "Classic", the group
			// pages
			// and the dashboard pages for the logged-in user.
			UserPortalConfig userPortalConfig = userPortalConfigService
					.getUserPortalConfig(pcontext.getPortalOwner(), request
							.getRemoteUser());

			// Retrieving the base URL for the current portal
			// This would translate into /portal/private/classic or
			// /portal/public/classic
			// in a vanilla installation
			portalURI = pcontext.getPortalURI();

			// Get all the navigation nodes for the logged-in user
			List<PageNavigation> navigations = userPortalConfig
					.getNavigations();

			writer.write("<div id=\"menu\">\n");
			writer.write("<ul>\n");

			// Loop over all the navigation nodes, it includes:
			// site nodes, group nodes and personal nodes (dashboard)
			for (PageNavigation pageNavigation : navigations) {

				// Get the resource bundle to translate the page nodes names
				ResourceBundle res = resourceBundleManager
						.getNavigationResourceBundle(request.getLocale()
								.getLanguage(), pageNavigation.getOwnerType(),
								pageNavigation.getOwnerId());

				String label = pageNavigation.getOwnerId();
				// Owner type can be "group", "user" or "portal"
				if (pageNavigation.getOwnerType().equals("group"))
				{
					label = OrganizationUtils.getGroupLabel(pageNavigation.getOwnerId());
				}
				
				writer.write("<li><h2 title=\"" + pageNavigation.getOwnerType() + ":" + pageNavigation.getOwnerId() + "\">" +  
						label + "</h2>\n");
				writer.write("<ul>\n");

				// Loop over all the page nodes for a certain group of pages
				for (PageNode pageNode : pageNavigation.getNodes()) {
					printElements(writer, pageNode, res, request
							.getRemoteUser());
				}
				writer.write("</ul></li>\n");
			}
			writer.write("</ul>\n");
			writer.write("</div>\n");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Recursive methods to write the children of a pageNode
	 */
	private void printElements(Writer writer, PageNode parent,
			ResourceBundle res, String remoteUser) throws Exception {

		if (parent.isVisible()) {

			parent.setResolvedLabel(res);
			
			// Retrieve the page associated to the navigation node
			// We will use it to display in a tooltip
			// what appears in <title> on the produced HTML
			Page page = userPortalConfigService.getPage(parent
					.getPageReference(), remoteUser);
			if (page != null) {
				// portalURI + parent.getUri() gives us an absolute link to the
				// navigation node
				writer.write("<li><a href=\"" + portalURI + parent.getUri()
						+ "\" title=\"" + page.getTitle() + "\">"
						+ parent.getEncodedResolvedLabel() + "</a>\n");
			} else {
				writer.write("<li>" + parent.getEncodedResolvedLabel() + "\n");
			}
			if (parent.getChildren() != null
					&& parent.getChildren().size() != 0) {
				for (PageNode pageNode : parent.getChildren()) {
					writer.write("<ul>\n");
					printElements(writer, pageNode, res, remoteUser);
					writer.write("</ul>\n");
				}
			}
			writer.write("</li>\n");
		}
	}

	@Override
	public void doHeaders(RenderRequest request, RenderResponse response) {
		// Adding the Stylesheet in the portal HEAD
		Element css = response.createElement("link");
		css.setAttribute("id", "verticalMenu");
		css.setAttribute("type", "text/css");
		css.setAttribute("rel", "stylesheet");
		css.setAttribute("href", request.getContextPath()
				+ "/css/stylesheet.css");
		response.addProperty(MimeResponse.MARKUP_HEAD_ELEMENT, css);
	}

}
