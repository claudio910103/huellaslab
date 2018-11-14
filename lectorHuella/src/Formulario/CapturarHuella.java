/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Formulario;

import BD.ConexionBD;
import com.digitalpersona.onetouch.DPFPDataPurpose;
import com.digitalpersona.onetouch.DPFPFeatureSet;
import com.digitalpersona.onetouch.DPFPGlobal;
import com.digitalpersona.onetouch.DPFPSample;
import com.digitalpersona.onetouch.DPFPTemplate;
import com.digitalpersona.onetouch.capture.DPFPCapture;
import com.digitalpersona.onetouch.capture.event.DPFPDataAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPDataEvent;
import com.digitalpersona.onetouch.capture.event.DPFPErrorAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPReaderStatusEvent;
import com.digitalpersona.onetouch.capture.event.DPFPSensorAdapter;
import com.digitalpersona.onetouch.capture.event.DPFPSensorEvent;
import com.digitalpersona.onetouch.processing.DPFPEnrollment;
import com.digitalpersona.onetouch.processing.DPFPFeatureExtraction;
import com.digitalpersona.onetouch.processing.DPFPImageQualityException;
import com.digitalpersona.onetouch.verification.DPFPVerification;
import com.digitalpersona.onetouch.verification.DPFPVerificationResult;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.logging.Level;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 *
 * @author claudio
 */

public class CapturarHuella extends javax.swing.JFrame {

    /**
     * Creates new form CapturarHuella
     */
    private DPFPCapture lector = DPFPGlobal.getCaptureFactory().createCapture();
    private DPFPEnrollment reclutador = DPFPGlobal.getEnrollmentFactory().createEnrollment();
    private DPFPVerification verificador = DPFPGlobal.getVerificationFactory().createVerification();
    private DPFPTemplate template;
    public static String TEMPLATE_PROPERTY = "template";
    
    public DPFPFeatureSet featuresInscripcion;
    public DPFPFeatureSet featuresVerificacion;
    
    private ImageIcon imageicon;
    private TrayIcon trayicon;
    private SystemTray systemtray;
    
    public DPFPFeatureSet extraerCaracteristicas(DPFPSample sample, DPFPDataPurpose purpose){
        DPFPFeatureExtraction extractor = DPFPGlobal.getFeatureExtractionFactory().createFeatureExtraction();
        try {
            return extractor.createFeatureSet(sample, purpose);
        } catch (DPFPImageQualityException e) {
            return null;
        }
    }
    public Image crearImagenHuella(DPFPSample sample){
        return DPFPGlobal.getSampleConversionFactory().createImage(sample);
    }
    public void dibujarHuella(Image image){
        lblImagen.setIcon(new ImageIcon(image.getScaledInstance(lblImagen.getWidth(), lblImagen.getHeight(), image.SCALE_DEFAULT)));
        repaint();
    }
    public void estadoHuellas(){
        EnviarTexto("Muestra de huellas necesarias para guardar el template" +
        reclutador.getFeaturesNeeded());
    }
    public void EnviarTexto(String string){
        txtMensaje.setText(txtMensaje.getText() + "\n" +string);
//        System.out.println("EnviarTexto"+txtMensaje.getText() + "\n" +string);
    }
    public void start(){
        lector.startCapture();
        EnviarTexto("Utilizando lector de huella");
    }
    public void stop(){
        lector.stopCapture();
        EnviarTexto("No se esta usando el lector");
    }
    public DPFPTemplate getTemplate(){
        return template;
    }
    public void setTemplate(DPFPTemplate template){
        DPFPTemplate old = this.template;
        this.template = template;
        firePropertyChange(TEMPLATE_PROPERTY, old, template);
    }
    
    public void procesarCaptura(DPFPSample sample){
        featuresInscripcion = extraerCaracteristicas(sample, DPFPDataPurpose.DATA_PURPOSE_ENROLLMENT);
        featuresVerificacion = extraerCaracteristicas(sample, DPFPDataPurpose.DATA_PURPOSE_VERIFICATION);
        if(featuresInscripcion!=null){
            try {
                System.out.println("Las caracteristicas de la huella han sido creadas");
                reclutador.addFeatures(featuresInscripcion);
                Image image=crearImagenHuella(sample);
                dibujarHuella(image);
                
//                btnVerificar.setEnabled(true);
                btnIdentificar.setEnabled(true);
            } catch (DPFPImageQualityException e) {
                System.out.println("Error: " + e.getMessage());
            }finally{
                estadoHuellas();
                System.out.println(reclutador.getTemplateStatus());
                switch(reclutador.getTemplateStatus()){
                    case TEMPLATE_STATUS_READY:
                        stop();
                        setTemplate(reclutador.getTemplate());
                        EnviarTexto("La plantilla de la huella ha sido creada, ya puede verificarla o identificarla");
                        btnIdentificar.setEnabled(false);
//                        btnVerificar.setEnabled(false);
                        btnGuardar.setEnabled(true);
                        btnGuardar.grabFocus();
                        break;
                    case TEMPLATE_STATUS_FAILED:
                        reclutador.clear();
                        stop();
                        estadoHuellas();
                        setTemplate(null);
                        JOptionPane.showMessageDialog(CapturarHuella.this, "La huella no pudo ser creada, intente nuevamente");
                }
            }
        }
    }
    
    protected void iniciar(){
        lector.addDataListener(new DPFPDataAdapter(){
            @Override public void dataAcquired(final DPFPDataEvent e){
                SwingUtilities.invokeLater(new Runnable(){
                    @Override public void run(){
                        EnviarTexto("La huella ha sido capturada ");
                        System.out.println("La huella ha sido capturada ");
                        procesarCaptura(e.getSample());
                    }
                });
            }
        });
        lector.addReaderStatusListener(new DPFPReaderStatusAdapter(){
            @Override public void readerConnected(final DPFPReaderStatusEvent e){
                SwingUtilities.invokeLater(new Runnable(){
                    @Override public void run(){
                        EnviarTexto("El sensor esta activado o conectado");
                    }
                });
            }
            @Override public void readerDisconnected(final DPFPReaderStatusEvent e){
                SwingUtilities.invokeLater(new Runnable(){
                    @Override public void run(){
                        EnviarTexto("El sensor esta desactivado o no conectado");
                    }
                });
            }
        });
        lector.addSensorListener(new DPFPSensorAdapter(){
            @Override public void fingerTouched(final DPFPSensorEvent e){
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        EnviarTexto("El dedo ha sido colocado sobre el lector de huella");
                    }
                });
            }
            @Override public void fingerGone(final DPFPSensorEvent e){
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        EnviarTexto("El dedo ha sido quitado del lector de huella");
                    }
                });
            }
        });
        lector.addErrorListener(new DPFPErrorAdapter(){
            //@Override
            public void errorReader(final DPFPErrorAdapter e){
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        EnviarTexto("Error: " + e.toString());
                    }
                });
            }
        });
    }
    
    ConexionBD con=new ConexionBD();
    public void guardarHuella(){
        ByteArrayInputStream datosHuella=new ByteArrayInputStream(template.serialize());
        Integer tamanoHuella=template.serialize().length;
        String nombre=JOptionPane.showInputDialog("Nombre: ");
        try {
            Connection c=con.conectar();
            PreparedStatement guardarStmt=c.prepareStatement("insert into somhue (huenombre,huehuella) values (?,?)");
            guardarStmt.setString(1, nombre);
            guardarStmt.setBinaryStream(2, datosHuella, tamanoHuella);
            guardarStmt.execute();
            guardarStmt.close();
            JOptionPane.showMessageDialog(null, "huella guardada correctamente");
            con.desconectar();
            btnGuardar.setEnabled(false);
//            btnVerificar.grabFocus();
        } catch (SQLException e) {
            System.err.println("Error al guardar los datos de la huella");
            System.err.println(e.getMessage());
        }finally{
            con.desconectar();
        }
    }
    
    public void verificarHuella(String userID){
        try {
            Connection c = con.conectar();
            String query = "select Huella from HUELLAS where UsuarioID = ? ";
            System.out.println(query.replace("?", userID));
            PreparedStatement verificarStmt = c.prepareStatement(query);
            verificarStmt.setString(1, userID);
            ResultSet rs = verificarStmt.executeQuery();
            while (rs.next()) {
                byte templateBuffer[] = rs.getBytes("huehuella");
                DPFPTemplate referenceTemplate=DPFPGlobal.getTemplateFactory().createTemplate(templateBuffer);
                setTemplate(referenceTemplate);
                DPFPVerificationResult result=verificador.verify(featuresVerificacion, getTemplate());
                if (result.isVerified()) {
                    JOptionPane.showMessageDialog(null, "La huella capturara coincide con la de "+userID);
                }else{
                    JOptionPane.showMessageDialog(null, "La huella no coincide con la de "+userID);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al verificar los datos de la huella");
            System.err.println(e.getMessage());
        }finally{
            con.desconectar();
        }
    }
    
    public CapturarHuella() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Imposible cambiar el tema" + e.toString(),"Error",JOptionPane.ERROR_MESSAGE);
        }
        imageicon = new ImageIcon(this.getClass().getResource("/images/logo.png"));
        initComponents();
        this.setIconImage(imageicon.getImage());
        instanciarTray();
        
        try{
            if(SystemTray.isSupported()){
                systemtray.add(trayicon);
                this.setVisible(false);
            }
        }catch(Exception e){
            System.out.println(e);
        }
    }
    
    private void instanciarTray(){
        trayicon = new TrayIcon(imageicon.getImage(),"Click derecho para ver opciones",popup);
        trayicon.setImageAutoSize(true);
        systemtray = SystemTray.getSystemTray();
    }
    
    public void identificarHuella() throws IOException{
        try {
            Connection c=con.conectar();
            PreparedStatement identificarStmt = c.prepareStatement("select Huella, UsuarioID  from HUELLAS  order by FechaAlta desc ");            
            ResultSet rs = identificarStmt.executeQuery();
            boolean isNoCapturada = true;
            while (rs.next()) {                
                byte templateBuffer[]=rs.getBytes("Huella");
                String usuarioID = rs.getString("UsuarioID");
                DPFPTemplate referenceTemplate = DPFPGlobal.getTemplateFactory().createTemplate(templateBuffer);
                
                setTemplate(referenceTemplate);
                DPFPVerificationResult result = verificador.verify(featuresVerificacion, getTemplate());
                if (result.isVerified()) {
                    //JOptionPane.showMessageDialog(null, "La huella capturada es del Usuario " + nombre,"Verificacion Huella",JOptionPane.INFORMATION_MESSAGE);
                    System.out.println("La huella capturada pertenece al Usuario:"+usuarioID);
                    isNoCapturada = false;
                    break;
                }
            }
            if(isNoCapturada){
                //JOptionPane.showMessageDialog(null, "La huella no reconocida, posiblemente aun no capturada" ,"Verificacion Huella",JOptionPane.INFORMATION_MESSAGE);
                System.out.println("Huella no reconocida");
            }
        } catch (SQLException e) {
            System.err.println("Error al identificar huella dactilar. "+e.getMessage());
        }finally{
            con.desconectar();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        popup = new java.awt.PopupMenu();
        restaurar = new java.awt.MenuItem();
        lblImagen = new javax.swing.JLabel();
        btnVerificar = new javax.swing.JButton();
        btnGuardar = new javax.swing.JButton();
        btnIdentificar = new javax.swing.JButton();
        btnSalir = new javax.swing.JButton();
        txtMensaje = new javax.swing.JTextField();
        btnSegundoPlano = new javax.swing.JButton();

        popup.setLabel("popupMenu1");
        popup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popupActionPerformed(evt);
            }
        });

        restaurar.setLabel("Restaurar Ventana");
        restaurar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                restaurarActionPerformed(evt);
            }
        });
        popup.add(restaurar);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(252, 252, 252));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        btnVerificar.setText("Verificar");
        btnVerificar.setEnabled(false);
        btnVerificar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnVerificarActionPerformed(evt);
            }
        });

        btnGuardar.setText("Guardar");
        btnGuardar.setEnabled(false);
        btnGuardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnGuardarActionPerformed(evt);
            }
        });

        btnIdentificar.setText("Identificar");
        btnIdentificar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnIdentificarActionPerformed(evt);
            }
        });

        btnSalir.setText("Salir");
        btnSalir.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSalirActionPerformed(evt);
            }
        });

        btnSegundoPlano.setText("Segundo Plano");
        btnSegundoPlano.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSegundoPlanoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(42, 42, 42)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(txtMensaje, javax.swing.GroupLayout.PREFERRED_SIZE, 297, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(btnIdentificar)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(btnSegundoPlano))
                                    .addComponent(btnVerificar))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(btnGuardar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(btnSalir, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(108, 108, 108)
                        .addComponent(lblImagen, javax.swing.GroupLayout.PREFERRED_SIZE, 157, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(61, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblImagen, javax.swing.GroupLayout.PREFERRED_SIZE, 152, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnVerificar)
                    .addComponent(btnGuardar))
                .addGap(28, 28, 28)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnIdentificar)
                    .addComponent(btnSalir)
                    .addComponent(btnSegundoPlano))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 42, Short.MAX_VALUE)
                .addComponent(txtMensaje, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSalirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSalirActionPerformed
        // TODO add your handling code here:
        System.exit(0);
    }//GEN-LAST:event_btnSalirActionPerformed

    private void btnVerificarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnVerificarActionPerformed
        // TODO add your handling code here:
        String nombre=JOptionPane.showInputDialog("UsuarioID a Verificar");
        verificarHuella(nombre);
        reclutador.clear();
    }//GEN-LAST:event_btnVerificarActionPerformed

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        iniciar();
        start();
        estadoHuellas();
        btnGuardar.setEnabled(false);
        btnIdentificar.setEnabled(false);
//        btnVerificar.setEnabled(false);
        btnSalir.grabFocus();
    }//GEN-LAST:event_formWindowOpened

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        // TODO add your handling code here:
        stop();
    }//GEN-LAST:event_formWindowClosing

    private void btnGuardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnGuardarActionPerformed
        try {
            guardarHuella();
            reclutador.clear();
            lblImagen.setIcon(null);
            start();
        } catch (Exception e) {
            Logger.getLogger(CapturarHuella.class.getName()).log(Level.SEVERE, null, e);
        }
    }//GEN-LAST:event_btnGuardarActionPerformed

    private void btnIdentificarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnIdentificarActionPerformed
        try {
            identificarHuella();
            reclutador.clear();
        } catch (IOException e) {
            //Logger.getLogger(CapturarHuella.class.getName()).log(Level.SEVERE, null, e);
        }
    }//GEN-LAST:event_btnIdentificarActionPerformed

    private void popupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_popupActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_popupActionPerformed

    private void btnSegundoPlanoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSegundoPlanoActionPerformed
        try{
            if(SystemTray.isSupported()){
                systemtray.add(trayicon);
                this.setVisible(false);
            }
        }catch(Exception e){
            System.out.println(e);
        }
    }//GEN-LAST:event_btnSegundoPlanoActionPerformed

    private void restaurarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_restaurarActionPerformed
        systemtray.remove(trayicon);
        this.setVisible(true);
    }//GEN-LAST:event_restaurarActionPerformed


    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(CapturarHuella.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(CapturarHuella.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(CapturarHuella.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(CapturarHuella.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new CapturarHuella().setVisible(false);                
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnGuardar;
    private javax.swing.JButton btnIdentificar;
    private javax.swing.JButton btnSalir;
    private javax.swing.JButton btnSegundoPlano;
    private javax.swing.JButton btnVerificar;
    private javax.swing.JLabel lblImagen;
    private java.awt.PopupMenu popup;
    private java.awt.MenuItem restaurar;
    private javax.swing.JTextField txtMensaje;
    // End of variables declaration//GEN-END:variables
}
