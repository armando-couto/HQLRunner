package org.lsbd.sgppd.util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.hql.internal.ast.ASTQueryTranslatorFactory;
import org.hibernate.hql.spi.QueryTranslator;
import org.hibernate.hql.spi.QueryTranslatorFactory;
import org.hibernate.jpa.internal.EntityManagerImpl;

public class JPQLRunner extends JFrame implements ActionListener, ListSelectionListener {

    private static final long serialVersionUID = 1L;

    private EntityManagerFactory factory;
    private EntityManager entityManager;

    // all containers
    private JPanel pnlTop = new JPanel(new BorderLayout());
    private JPanel pnlBottom = new JPanel(new BorderLayout());
    private JSplitPane pnlSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, pnlTop, pnlBottom);

    // all components
    private JTextArea txtHQL = new JTextArea(6, 100);

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private JComboBox cmbLimit = new JComboBox(new String[] { "All", "100", "1000", "10000" });
    private JButton btnRunHQL = new JButton("Run HQL Query");
    private JButton btnRunSQL = new JButton("Run SQL Query");

    private JTable tblResult = new JTable();
    private JTextArea txtGeneratedSQL = new JTextArea();
    @SuppressWarnings("rawtypes")
    private JList lstHistory = new JList();

    private List<String> lstQueryHistory = new ArrayList<String>();

    @SuppressWarnings("serial")
    private JPQLRunner() {

        factory = Persistence.createEntityManagerFactory("default");
        entityManager = factory.createEntityManager();

        this.setMinimumSize(new Dimension(800, 600));
        this.setTitle("HQL Runner");

        this.pnlTop.setBorder(new javax.swing.border.TitledBorder(
                "Enter your HQL below and hit 'Run'"));
        this.pnlTop.add(this.txtHQL, BorderLayout.CENTER);
        this.cmbLimit.setSelectedIndex(1); // limit 100 is selected by
                                           // default...
        this.btnRunHQL.addActionListener(this);
        this.btnRunSQL.addActionListener(this);

        this.pnlTop.add(new JPanel() {
            {
                add(new JLabel(" Limit"));
                add(cmbLimit);
                add(new JLabel("Rows "));
                add(btnRunHQL);
                add(btnRunSQL);
            }
        }, BorderLayout.SOUTH);

        this.pnlBottom.add(new JTabbedPane() {
            {
                addTab("Result", new JScrollPane(tblResult));
                addTab("Generated SQL", new JScrollPane(txtGeneratedSQL));
                addTab("History", lstHistory);
            }
        }, BorderLayout.CENTER);

        lstHistory.addListSelectionListener(this);
        lstHistory.setSelectionMode(DefaultListSelectionModel.SINGLE_SELECTION);

        this.pnlSplit.setDividerLocation(0.4d);

        this.getContentPane().add(this.pnlSplit);

        this.setVisible(true);
    }

    @SuppressWarnings("rawtypes")
    public void runHQL(String strHQL) {
        this.addToHistory(strHQL);
        this.txtGeneratedSQL.setText(this.toSql(strHQL));
        if (!entityManager.isOpen())
            entityManager.getTransaction().begin();
        Query query = entityManager.createQuery(strHQL);
        if (!"All".equals(this.cmbLimit.getSelectedItem()))
            query.setMaxResults(Integer.parseInt((String) this.cmbLimit.getSelectedItem()));
        List list = query.getResultList();
        this.setResult(list);
    }

    @SuppressWarnings("rawtypes")
    public void runSQL(String strHQL) {
        this.addToHistory(strHQL);
        if (!entityManager.isOpen())
            entityManager.getTransaction().begin();
        Query query = entityManager.createNativeQuery(strHQL);
        if (!"All".equals(this.cmbLimit.getSelectedItem()))
            query.setMaxResults(Integer.parseInt((String) this.cmbLimit.getSelectedItem()));
        List lst = query.getResultList();
        this.setResult(lst);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void addToHistory(String strHQL) {
        lstQueryHistory.add(strHQL);
        lstHistory.setModel(new ListModel() {
            @Override
            public void removeListDataListener(ListDataListener arg0) {
            }

            @Override
            public int getSize() {
                return lstQueryHistory.size();
            }

            @Override
            public Object getElementAt(int index) {
                return lstQueryHistory.get(index);
            }

            @Override
            public void addListDataListener(ListDataListener arg0) {
            }
        });
    }

    @SuppressWarnings("rawtypes")
    private void setResult(final List lst) {
        tblResult.setModel(new TableModel() {

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            }

            @Override
            public void removeTableModelListener(TableModelListener l) {
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                Object row = lst.get(rowIndex);
                if (row instanceof Object[]) {
                    Object[] cols = (Object[]) row;
                    return cols[columnIndex];
                } else {
                    try {
                        BeanInfo testBeanInfo = Introspector.getBeanInfo(row.getClass());
                        return testBeanInfo.getPropertyDescriptors()[columnIndex].getReadMethod()
                                .invoke(row);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        return row;
                    }
                }
            }

            @Override
            public int getRowCount() {
                return lst.size();
            }

            @Override
            public String getColumnName(int columnIndex) {
                if (lst.size() > 0) {
                    Object row = lst.get(0);
                    if (row instanceof Object[]) {
                        return ((Object[]) row)[columnIndex].getClass().getName();
                    } else {
                        try {
                            BeanInfo testBeanInfo = Introspector.getBeanInfo(row.getClass());
                            return testBeanInfo.getPropertyDescriptors()[columnIndex].getName();
                        } catch (IntrospectionException e) {
                            e.printStackTrace();
                            return "????";
                        }
                    }
                }
                return "";
            }

            @Override
            public int getColumnCount() {
                if (lst.size() > 0) {
                    Object row = lst.get(0);
                    if (row instanceof Object[]) {
                        return ((Object[]) row).length;
                    } else {
                        try {
                            BeanInfo testBeanInfo = Introspector.getBeanInfo(row.getClass());
                            return testBeanInfo.getPropertyDescriptors().length;
                        } catch (IntrospectionException e) {
                            e.printStackTrace();
                            return 0;
                        }
                    }
                }
                return 0;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (lst.size() > 0) {
                    Object row = lst.get(0);
                    if (row instanceof Object[]) {
                        return ((Object[]) row)[columnIndex].getClass();
                    } else
                        return Object.class;
                }
                return null;
            }

            @Override
            public void addTableModelListener(TableModelListener l) {

            }
        });
    }

    public String toSql(String hqlQueryText) {
        if (hqlQueryText != null && hqlQueryText.trim().length() > 0) {
            final QueryTranslatorFactory translatorFactory = new ASTQueryTranslatorFactory();
            final EntityManagerImpl factory = (EntityManagerImpl) entityManager;
            final QueryTranslator translator = translatorFactory.createQueryTranslator(
                    hqlQueryText, hqlQueryText, java.util.Collections.EMPTY_MAP,
                    (SessionFactoryImplementor) factory.getSession().getSessionFactory(), null);
            translator.compile(java.util.Collections.EMPTY_MAP, false);
            return translator.getSQLString();
        }
        return null;
    }

    /**** Event-Handlers ****/
    @SuppressWarnings("rawtypes")
    public void actionPerformed(ActionEvent e) {
        this.setResult(new ArrayList());
        this.txtGeneratedSQL.setText("");
        if (e.getSource() == btnRunHQL)
            this.runHQL(txtHQL.getText());
        else if (e.getSource() == btnRunSQL)
            this.runSQL(txtHQL.getText());
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        txtHQL.setText(lstQueryHistory.get(lstHistory.getSelectedIndex()));
    }

    /**** Main Method ****/
    public static void main(String[] args) {
        JPQLRunner runner = new JPQLRunner();
        runner.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}