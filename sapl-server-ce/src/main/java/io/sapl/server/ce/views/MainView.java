package io.sapl.server.ce.views;

import java.util.Arrays;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteData;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

import io.sapl.server.ce.views.documentation.FunctionLibrariesDocumentationView;
import io.sapl.server.ce.views.documentation.PolicyInformationPointsDocumentationView;
import io.sapl.server.ce.views.pdpconfiguration.ConfigurePdp;
import io.sapl.server.ce.views.sapldocument.SaplDocumentsView;

/**
 * The main view is a top-level placeholder for other views.
 */
@Push
@JsModule("./styles/shared-styles.js")
@Theme(value = Lumo.class, variant = Lumo.DARK)
@CssImport("styles/views/main/main-view.css")
public class MainView extends AppLayout {

	private final Tabs menu;
	private H1 viewTitle;

	public MainView() {
		setPrimarySection(Section.DRAWER);
		addToNavbar(true, createHeaderContent());
		menu = createMenu();
		addToDrawer(createDrawerContent(menu));
	}

	private Component createHeaderContent() {
		HorizontalLayout layout = new HorizontalLayout();
		layout.setId("header");
		layout.getThemeList().set("dark", true);
		layout.setWidthFull();
		layout.setSpacing(false);
		layout.setAlignItems(FlexComponent.Alignment.CENTER);
		layout.add(new DrawerToggle());
		viewTitle = new H1();
		layout.add(viewTitle);
		return layout;
	}

	private Component createDrawerContent(Tabs menu) {
		VerticalLayout layout = new VerticalLayout();
		layout.setSizeFull();
		layout.setPadding(false);
		layout.setSpacing(false);
		layout.getThemeList().set("spacing-s", true);
		layout.setAlignItems(FlexComponent.Alignment.STRETCH);
		HorizontalLayout logoLayout = new HorizontalLayout();
		logoLayout.setId("logo");
		logoLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		logoLayout.add(new Image("images/logos/18.png", "SAPL Server CE logo"));
		logoLayout.add(new H1("SAPL Server CE"));
		layout.add(logoLayout, menu);
		return layout;
	}

	private Tabs createMenu() {
		final Tabs tabs = new Tabs();
		tabs.setOrientation(Tabs.Orientation.VERTICAL);
		tabs.addThemeVariants(TabsVariant.LUMO_MINIMAL);
		tabs.setId("tabs");
		tabs.add(createMenuItems());
		return tabs;
	}

	private Component[] createMenuItems() {
		RouterLink[] links = new RouterLink[] { new RouterLink("Home", ShowHome.class),
				new RouterLink("SAPL Documents", SaplDocumentsView.class),
				new RouterLink("PDP Configuration", ConfigurePdp.class),
				new RouterLink("Policy Information Points", PolicyInformationPointsDocumentationView.class),
				new RouterLink("Function Libraries", FunctionLibrariesDocumentationView.class),
				new RouterLink("Client Credentials", ListClientCredentials.class) };
		return Arrays.stream(links).map(MainView::createTab).toArray(Tab[]::new);
	}

	private static Tab createTab(Component content) {
		final Tab tab = new Tab();
		tab.add(content);
		return tab;
	}

	@Override
	protected void afterNavigation() {
		super.afterNavigation();
		updateChrome();
	}

	private void updateChrome() {
		getTabWithCurrentRoute().ifPresent(menu::setSelectedTab);
		viewTitle.setText(getCurrentPageTitle());
	}

	private Optional<Tab> getTabWithCurrentRoute() {
		String currentRoute = getCurrentRoute();
		return menu.getChildren().filter(tab -> hasLink(tab, currentRoute)).findFirst().map(Tab.class::cast);
	}

	private String getCurrentRoute() {
		// RouteConfiguration.forSessionScope().getUrl(getContent().getClass()) does not
		// work for views with URL parameter
		Class<?> clazz = getContent().getClass();

		RouteConfiguration routeConfiguration = RouteConfiguration.forSessionScope();
		for (RouteData routeData : routeConfiguration.getAvailableRoutes()) {
			if (routeData.getNavigationTarget().equals(clazz)) {
				return routeData.getUrl();
			}
		}

		throw new IllegalStateException(String.format("no route is available for %s", clazz));
	}

	private boolean hasLink(Component tab, String currentRoute) {
		return tab.getChildren().filter(RouterLink.class::isInstance).map(RouterLink.class::cast)
				.map(RouterLink::getHref).anyMatch(currentRoute::equals);
	}

	private String getCurrentPageTitle() {
		return getContent().getClass().getAnnotation(PageTitle.class).value();
	}
}
