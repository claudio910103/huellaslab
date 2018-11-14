/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package BD;
/**
 *
 * @author leonskb4
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;
public class ConexionBD {
    public String puerto="3306";
    public String nomServidor="104.248.237.128";
    public String db="easylab";
    public String user="itswa";
    public String pass="123456";
    Connection conn=null;
    
    public Connection conectar(){
        try {
            String ruta="jdbc:mysql://";
            String servidor=nomServidor+":"+puerto+"/"+db+"?useSSL=false&serverTimezone=UTC&";
            Class.forName("com.mysql.jdbc.Driver");
            String conexion = ruta+servidor+db+user+pass;
            System.out.println(conexion);
            conn = DriverManager.getConnection(ruta+servidor,user,pass);
            if(conn!=null){
                System.out.println("Conexión a BD... listo!!!");
            }else if(conn==null){
                throw new SQLException();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, e.getMessage());
        }catch(ClassNotFoundException e){
            JOptionPane.showMessageDialog(null, "Se produjo el sgte. error: "+e.getMessage());
        }catch(NullPointerException e){
            JOptionPane.showMessageDialog(null, "Se produjo el sgte. error: "+e.getMessage());
        }finally{
            return conn;
        }
    }
    
    public void desconectar(){
        conn = null;
        System.out.println("Desconexion... listo!!!");
    }
    
    
}
