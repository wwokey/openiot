package org.openiot.ui.request.presentation.web.model.nodes.impl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.component.html.HtmlOutputText;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

import org.openiot.commons.osdspec.model.PresentationAttr;
import org.openiot.commons.sdum.serviceresultset.model.SdumServiceResultSet;
import org.openiot.commons.sparql.result.model.Binding;
import org.openiot.commons.sparql.result.model.Result;
import org.openiot.ui.request.presentation.web.model.nodes.interfaces.VisualizationWidget;
import org.openiot.ui.request.presentation.web.util.FaceletLocalization;
import org.primefaces.component.commandlink.CommandLink;
import org.primefaces.component.panel.Panel;
import org.primefaces.context.RequestContext;
import org.primefaces.model.chart.CartesianChartModel;
import org.primefaces.model.chart.ChartSeries;

public class LineChart implements VisualizationWidget {

	private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
	private org.primefaces.component.chart.line.LineChart widget;
	private HtmlOutputText emptyMessage;
	private Panel panel;
	private CartesianChartModel model;
	private String title;
	private String xAxisLabel;
	private String yAxisLabel;
	private int numSeries;
	private String[] seriesLabels;

	@Override
	public UIComponent createWidget(List<PresentationAttr> presentationAttributes) {
		FacesContext fc = FacesContext.getCurrentInstance();
		Application application = fc.getApplication();

		parseAttributes(presentationAttributes);

		// Instanciate linechart widget
		widget = (org.primefaces.component.chart.line.LineChart) application.createComponent(fc, "org.primefaces.component.chart.LineChart", "org.primefaces.component.chart.LineChartRenderer");
		widget.setId("lineChart_" + System.nanoTime());
		widget.setStyleClass("line-chart");
		widget.setXaxisLabel(xAxisLabel);
		widget.setYaxisLabel(yAxisLabel);
		widget.setXaxisAngle(90);
		widget.setShowMarkers(true);
		widget.setLegendPosition("s");
		widget.setRendered(false);

		// Instanciate a panel to host the widget
		panel = (Panel) application.createComponent(fc, "org.primefaces.component.Panel", "org.primefaces.component.PanelRenderer");
		panel.setId("widget_panel_" + System.nanoTime());
		panel.setHeader(title);
		panel.setClosable(false);
		panel.setToggleable(false);
		panel.setStyleClass("widget");
		panel.getChildren().add(widget);

		// Generate clear data link
		CommandLink clearLink = (CommandLink) application.createComponent(fc, "org.primefaces.component.CommandLink", "org.primefaces.component.CommandLinkRenderer");
		clearLink.setAjax(true);
		clearLink.setOnclick("windowBlockUI.block();");
		clearLink.setOncomplete("windowBlockUI.unblock();");
		clearLink.setProcess("@this");
		clearLink.setStyleClass("ui-panel-titlebar-icon ui-corner-all ui-state-default");
		HtmlOutputText btn = new HtmlOutputText();
		btn.setStyleClass("ui-icon ui-icon-arrowrefresh-1-w");
		clearLink.getChildren().add(btn);
		clearLink.addActionListener(new ActionListener() {

			@Override
			public void processAction(ActionEvent arg0) throws AbortProcessingException {
				clearData();
			}
		});
		panel.getFacets().put("actions", clearLink);

		// Generate empty message
		emptyMessage = new HtmlOutputText();
		emptyMessage.setValue(FaceletLocalization.getLocalizedResourceBundle().getString("UI_WIDGET_NO_DATA"));
		emptyMessage.setStyleClass("no-data");
		emptyMessage.setRendered(true);
		panel.getChildren().add(emptyMessage);

		// Setup model
		model = new CartesianChartModel();
		for (String seriesLabel : seriesLabels) {
			model.addSeries(new ChartSeries(seriesLabel));
		}
		widget.setValue(model);

		return panel;
	}

	@Override
	public void processData(SdumServiceResultSet resultSet) {
		boolean triggerUpdate = false;

		for (Result result : resultSet.getQueryResult().getSparql().getResults().getResult()) {
			// Parse data
			Object xValue = sdf.format(new Date());
			Double[] yValues = new Double[numSeries];
			for (Binding binding : result.getBinding()) {
				if ("x".equals(binding.getName())) {
					xValue = Double.valueOf(binding.getLiteral().getContent());
				} else if (binding.getName().startsWith("y")) {
					// y values start at index 1 (y1, y2 e.t.c)
					int yValueIndex = Integer.valueOf(binding.getName().substring(1)) - 1;
					yValues[yValueIndex] = Double.valueOf(binding.getLiteral().getContent());
				}
			}

			// Update series
			for (int seriesIndex = 0; seriesIndex < numSeries; seriesIndex++) {
				ChartSeries series = model.getSeries().get(seriesIndex);
				Double yValue = yValues[seriesIndex];
				if (yValue == null) {
					continue;
				}
				series.getData().put(xValue, yValue);
				triggerUpdate = true;
			}
		}

		if (triggerUpdate) {
			widget.setRendered(true);
			emptyMessage.setRendered(false);
			RequestContext requestContext = RequestContext.getCurrentInstance();
			if (requestContext != null) {
				requestContext.update(panel.getClientId());
			}
		}

	}

	private void parseAttributes(List<PresentationAttr> presentationAttributes) {
		seriesLabels = null;
		// Figure out number of series
		for (PresentationAttr attr : presentationAttributes) {
			if ("SERIES".equals(attr.getName())) {
				numSeries = Integer.valueOf(attr.getValue());
				seriesLabels = new String[numSeries];
				break;
			}
		}
		//
		for (PresentationAttr attr : presentationAttributes) {
			if ("TITLE".equals(attr.getName())) {
				title = attr.getValue();
			} else if ("X_AXIS_LABEL".equals(attr.getName())) {
				xAxisLabel = attr.getValue();
			} else if ("Y_AXIS_LABEL".equals(attr.getName())) {
				yAxisLabel = attr.getValue();
			} else if (attr.getName().startsWith("SERIES_")) {
				int seriesIndex = Integer.valueOf(attr.getName().split("_")[1]);
				seriesLabels[seriesIndex] = attr.getValue();
			}
		}
	}

	@Override
	public void clearData() {
		for (ChartSeries series : model.getSeries()) {
			series.getData().clear();
		}
		
		widget.setRendered(false);
		emptyMessage.setRendered(true);

		RequestContext requestContext = RequestContext.getCurrentInstance();
		if (requestContext != null) {
			requestContext.update(panel.getClientId());
		}
	}

}
