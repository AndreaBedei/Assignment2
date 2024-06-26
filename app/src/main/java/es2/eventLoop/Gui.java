package es2.eventLoop;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Gui extends JFrame {
    private JTextField txtWebAddress, txtWord, txtDepth;
    private JButton buttonSearch, buttonStop;
    private DefaultTableModel model;
    private JTable table;
    private java.util.List<VerticleSearch_v2> listOfVerticles = new ArrayList<>();
    private Vertx vertx;
    private AtomicBoolean stopRequested = new AtomicBoolean(false);

    public Gui() {
        setTitle("Esempio GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 400);

        // Creazione dei campi di testo
        JLabel labelWebAddress = new JLabel("Web address:");
        JLabel labelWord = new JLabel("Word:");
        JLabel labelDepth = new JLabel("Depth:");
        txtWebAddress = new JTextField("https://virtuale.unibo.it", 30);
        txtWord = new JTextField("virtuale", 30);
        txtDepth = new JTextField("2", 30);

        Object[][] data = {};
        String[] columnNames = {"Address", "Depth", "Num"};
        model = new DefaultTableModel(data, columnNames);

        table = new JTable(model);
        table.getColumnModel().getColumn(1).setMaxWidth(60);
        table.getColumnModel().getColumn(2).setMaxWidth(60);
        var center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(1).setCellRenderer(center);
        table.getColumnModel().getColumn(2).setCellRenderer(center);
        
        
        // Creazione dei pulsanti
        buttonSearch = new JButton("Search");
        buttonStop = new JButton("Stop");
        buttonStop.setEnabled(false);
        
        int borderSize = 10;

        // Aggiunta degli elementi al content pane
        Container container = getContentPane();
        container.setLayout(new BorderLayout(0, borderSize));

        // Pannello per gli input
        JPanel topPanel = new JPanel(new GridLayout(3, 2));
        topPanel.setPreferredSize(new Dimension(300, 80));
        topPanel.add(labelWebAddress);
        topPanel.add(txtWebAddress);
        topPanel.add(labelWord);
        topPanel.add(txtWord);
        topPanel.add(labelDepth);
        topPanel.add(txtDepth);

        // Pannello per la tabella
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(350, 200));
        
        // Pannello per i pulsanti
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(buttonSearch);
        bottomPanel.add(buttonStop);

        container.add(topPanel, BorderLayout.NORTH);
        container.add(scrollPane, BorderLayout.CENTER);
        container.add(bottomPanel, BorderLayout.SOUTH);

        buttonSearch.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.setRowCount(0);
                buttonSearch.setEnabled(false);
                buttonSearch.setText("Searching...");
                buttonStop.setEnabled(true);

                String webAddress = txtWebAddress.getText();
                int depth = Integer.parseInt(txtDepth.getText());
                String word = txtWord.getText();

                vertx = Vertx.vertx();
                listOfVerticles.clear();
                stopRequested.set(false);
                List<Future<String>> listOfFutures = new ArrayList<>();
                Set<String> alreadyVisitedPages = new ConcurrentSkipListSet<>();

                int nVerticle = 5;

                // Creation of the verticles.
                for(int i = 0; i < nVerticle; i++){
                    VerticleSearch_v2 verticle = new VerticleSearch_v2(webAddress, depth, word, res -> SwingUtilities.invokeLater(() -> {
                        updateTextArea(res);
                    }), i, nVerticle, alreadyVisitedPages, stopRequested);

                    listOfVerticles.add(verticle);
                    listOfFutures.add(vertx.deployVerticle(verticle));
                }

                Future.any(listOfFutures)   // Only one of the verticles will complete the promise because only one will read remainingSearches == 0.
                .onComplete((res) -> {
                    buttonSearch.setEnabled(true);
                    buttonSearch.setText("Search");
                    buttonStop.setEnabled(false);
                    buttonStop.setText("Stop");
                });
            }
        });

        buttonStop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buttonStop.setEnabled(false);
                buttonStop.setText("Stopping...");
                stopRequested.set(true);
                
                // Stop all the verticles deployed and added to the list.
                listOfVerticles.forEach(v -> vertx.undeploy(v.deploymentID()));
            }
        });
    }

    public void updateTextArea(VerticleSearch_v2.Result res) {
        Object[] rowData = {res.webAddress(), res.depth(), res.occurrences()};
        model.addRow(rowData);
        table.scrollRectToVisible(table.getCellRect(table.getRowCount() - 1, 0, true));
    }
}
