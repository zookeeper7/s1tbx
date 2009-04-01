package org.esa.beam.visat.toolviews.layermanager.layersrc;

import org.esa.beam.framework.ui.assistant.AbstractAppAssistantPage;
import org.esa.beam.framework.ui.assistant.AppAssistantPageContext;
import org.esa.beam.visat.toolviews.layermanager.LayerSource;
import org.esa.beam.visat.toolviews.layermanager.LayerSourceDescriptor;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SelectLayerSourceAssistantPage extends AbstractAppAssistantPage {

    private JList list;
    private Map<LayerSourceDescriptor, LayerSource> controllerMap;

    public SelectLayerSourceAssistantPage(LayerSourceDescriptor[] sourceDescriptors) {
        super("Select Layer Source");
        controllerMap = new HashMap<LayerSourceDescriptor, LayerSource>();
        for (LayerSourceDescriptor sourceDescriptor : sourceDescriptors) {
            controllerMap.put(sourceDescriptor, sourceDescriptor.createLayerSource());
        }
    }

    @Override
    public boolean validatePage() {
        return list.getSelectedIndex() >= 0;
    }

    @Override
    public boolean hasNextPage() {
        LayerSourceDescriptor selected = (LayerSourceDescriptor) list.getSelectedValue();
        if (selected == null) {
            return false;
        }
        return controllerMap.get(selected).hasFirstPage();
    }

    @Override
    public AbstractAppAssistantPage getNextPage(AppAssistantPageContext pageContext) {
        LayerSourceDescriptor selected = (LayerSourceDescriptor) list.getSelectedValue();
        if (selected == null) {
            return null;
        }
        return controllerMap.get(selected).getFirstPage(pageContext);
    }

    @Override
    public boolean canFinish() {
        return !hasNextPage();
    }

    @Override
    public boolean performFinish(AppAssistantPageContext pageContext) {
        LayerSourceDescriptor selected = (LayerSourceDescriptor) list.getSelectedValue();
        if (selected == null) {
            return false;
        }
        return controllerMap.get(selected).finish(pageContext);
    }

    @Override
    public Component createLayerPageComponent(AppAssistantPageContext context) {
        Set<LayerSourceDescriptor> descriptorSet = controllerMap.keySet();
        List<LayerSourceDescriptor> descriptorList = new ArrayList<LayerSourceDescriptor>(descriptorSet.size());
        for (LayerSourceDescriptor lsd : descriptorSet) {
            LayerSource lsc = controllerMap.get(lsd);
            if (lsc.isApplicable(context)) {
                descriptorList.add(lsd);
            }
        }
        Collections.sort(descriptorList, new Comparator<LayerSourceDescriptor>() {
            @Override
            public int compare(LayerSourceDescriptor o1, LayerSourceDescriptor o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        list = new JList(descriptorList.toArray(new LayerSourceDescriptor[descriptorList.size()]));
        list.getSelectionModel().addListSelectionListener(new MyListSelectionListener(context));
        list.setCellRenderer(new MyDefaultListCellRenderer());

        GridBagConstraints gbc = new GridBagConstraints();
        final JPanel panel = new JPanel(new GridBagLayout());
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("Available layer sources:"), gbc);
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridy++;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(new JScrollPane(list), gbc);

        return panel;
    }

    private class MyListSelectionListener implements ListSelectionListener {

        private final AppAssistantPageContext pageContext;

        public MyListSelectionListener(AppAssistantPageContext pageContext) {
            this.pageContext = pageContext;
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            pageContext.updateState();
        }
    }

    private static class MyDefaultListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            final JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
                                                                             cellHasFocus);
            if (value instanceof LayerSourceDescriptor) {
                LayerSourceDescriptor layerSourceDescriptor = (LayerSourceDescriptor) value;
                label.setText("<html><b>" + layerSourceDescriptor.getName() + "</b>");
                label.setToolTipText(layerSourceDescriptor.getDescription());
            } else {
                label.setText("Invalid");
            }
            return label;
        }
    }

}
