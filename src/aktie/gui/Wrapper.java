package aktie.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Wrapper
{

    public static int RESTART_RC = 7;

    public static String VERSION_0115 = "version 0.1.15";
    public static String VERSION_0403 = "version 0.4.3";
    public static String VERSION_0405 = "version 0.4.5";
    public static String VERSION_0418 = "version 0.4.18";
    //public static String VERSION_0419 = "version 0.4.19";

    public static String VERSION = VERSION_0418;

    public static String VERSION_FILE = "version.txt";

    //ADD ONE HOUR TO TIME.
    //This makes sure this time value is greater than the time of
    //the upgrade file added to the network by the developer account.
    //This keeps new installs from downloading the same version as
    //an upgrade
    public static long RELEASETIME = ( 1465913170L * 1000L ) + 3600000L;

    //Hash cash payment values
    public static long OLDPAYMENT = 0;
    public static long CHECKNEWPAYMENTAFTER = ( 1465595547L * 1000L ) +
            ( 8L * 60L * 60L * 1000L );

    //                              0x0123456789ABCDEFL;
    public static long NEWPAYMENT = 0x0000004000000000L;

    public static String RUNDIR = "aktie_run_dir";
    public static String LIBDIR = RUNDIR + File.separator + "lib";
    //public static String JARFILE = "aktie.jar";

    public static void main ( String args[] )
    {
        int rc = RESTART_RC;

        while ( rc == RESTART_RC )
        {
            rc = Main ( args );
            System.out.println ( "RC: " + rc );
        }

    }

    public static int Main ( String args[] )
    {
        boolean verbose = false;
        boolean headless = false;

        for ( int ct = 0; ct < args.length; ct++ )
        {
            if ( "-v".equals ( args[ct] ) )
            {
                verbose = true;
            }

            if ( "-headless".equals ( args[ct] ) )
            {
                headless = true;
            }

        }

        //Check the system
        String systype = System.getProperty ( "os.name" );
        System.out.println ( "SYS: " + systype );
        //Test if rundir exists.
        File f = new File ( RUNDIR );

        boolean setstartonfirst = false;
        boolean usesemi = false;

        if ( systype.startsWith ( "Windows" ) )
        {
            usesemi = true;
        }

        if ( "Mac OS X".equals ( systype ) )
        {
            setstartonfirst = true;
        }

        if ( !f.exists() || isNewer() )
        {

            deleteLibDir(); //Delete old jar files in case of upgrade file names not matching
            unZipIt();

            List<String> cmd = new LinkedList<String>();

            cmd.add ( "java" );
            cmd.add ( "-version" );
            ProcessBuilder pb = new ProcessBuilder();
            pb.redirectErrorStream ( true );
            pb.command ( cmd );

            boolean is64bit = false;

            try
            {

                Matcher m = Pattern.compile ( "64-Bit" ).matcher ( "" );
                Process pc = pb.start();
                BufferedReader br = new BufferedReader ( new InputStreamReader ( pc.getInputStream () ) );
                String ln = br.readLine ();

                while ( ln != null )
                {
                    m.reset ( ln );

                    if ( m.find() )
                    {
                        is64bit = true;
                    }

                    ln = br.readLine ();

                }

            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

            if ( systype.startsWith ( "Linux" ) )
            {
                if ( is64bit )
                {
                    File sfile = new File ( RUNDIR + File.separator + "swt" + File.separator + "swt_linux_64.jar" );
                    File destfile = new File ( RUNDIR + File.separator + "lib" + File.separator + "swt_linux_64.jar" );
                    sfile.renameTo ( destfile );
                }

                else
                {
                    File sfile = new File ( RUNDIR + File.separator + "swt" + File.separator + "swt_linux.jar" );
                    File destfile = new File ( RUNDIR + File.separator + "lib" + File.separator + "swt_linux.jar" );
                    sfile.renameTo ( destfile );
                }

            }

            if ( "Mac OS X".equals ( systype ) )
            {
                File sfile = new File ( RUNDIR + File.separator + "swt" + File.separator + "swt_osx.jar" );
                File destfile = new File ( RUNDIR + File.separator + "lib" + File.separator + "swt_osx.jar" );
                sfile.renameTo ( destfile );
            }

            if ( systype.startsWith ( "Windows" ) )
            {
                if ( is64bit )
                {
                    File sfile = new File ( RUNDIR + File.separator + "swt" + File.separator + "swt_win_64.jar" );
                    File destfile = new File ( RUNDIR + File.separator + "lib" + File.separator + "swt_win_64.jar" );
                    sfile.renameTo ( destfile );
                }

                else
                {
                    File sfile = new File ( RUNDIR + File.separator + "swt" + File.separator + "swt_win.jar" );
                    File destfile = new File ( RUNDIR + File.separator + "lib" + File.separator + "swt_win.jar" );
                    sfile.renameTo ( destfile );
                }

            }

        }

        if ( !f.isDirectory() )
        {
            System.out.println ( "Oops.  I sorry.  I thought aktie should be a directory!" );
            System.exit ( 1 );
        }

        //Make bak dir
        File bd = new File ( RUNDIR + File.separator + "bak" );

        if ( !bd.exists() )
        {
            bd.mkdirs();
        }

        //Check for upgrades
        File updir = new File ( RUNDIR + File.separator + "upgrade" );

        if ( !updir.exists() )
        {
            updir.mkdirs();
        }

        File uplst[] = updir.listFiles();
        System.out.println ( "Upgrade list: " + uplst.length );

        for ( int c = 0; c < uplst.length; c++ )
        {
            File uf = uplst[c];

            String ufn = uf.getName();

            String len = getUpdateLength ( ufn );
            String rlen = Long.toString ( uf.length() );

            System.out.println ( "Checking length: " + ufn + "  " + rlen + " is expected: " + len );

            if ( len != null && len.equals ( rlen ) )
            {
                unZipUpgrade ( uf );

            }

            saveUpdateLength ( ufn, "-1" );
        }

        //Just run it!
        //java -XstartOnFirstThread -cp aktie.jar:aktie/lib/*:org.eclipse.swt/swt.jar aktie.gui.SWTApp aktie_node
        List<String> cmd = new LinkedList<String>();
        cmd.add ( "java" );

        if ( setstartonfirst )
        {
            cmd.add ( "-XstartOnFirstThread" );
        }

        if ( systype.startsWith ( "Linux" ) )
        {
            cmd.add ( "-DSWT_GTK3=0" );
        }

        cmd.add ( "-Xmx256m" );
        cmd.add ( "-cp" );
        StringBuilder sb = new StringBuilder();
        File libd = new File ( LIBDIR );
        System.out.println ( "LIST LIBS: " + libd.getPath() );
        File ll[] = libd.listFiles();

        if ( ll != null && ll.length > 0 )
        {

            sb.append ( ll[0] );

            for ( int c = 1; c < ll.length; c++ )
            {
                if ( ll[c].getPath().endsWith ( ".jar" ) )
                {
                    if ( usesemi )
                    {
                        sb.append ( ";" );
                    }

                    else
                    {
                        sb.append ( ":" );
                    }

                    sb.append ( ll[c] );
                }

            }

        }

        cmd.add ( sb.toString() );

        if ( !headless )
        {
            cmd.add ( "aktie.gui.SWTApp" );
        }

        else
        {
            cmd.add ( "aktie.headless.HeadlessMain" );
        }

        cmd.add ( RUNDIR + File.separator + "aktie_node" );

        if ( verbose )
        {
            cmd.add ( "-v" );
            System.out.println ( "SETTING VERBOSE!" );
        }

        ProcessBuilder pb = new ProcessBuilder();
        pb.redirectErrorStream ( true );
        pb.command ( cmd );

        try
        {
            Process pc = pb.start();
            pc.getInputStream();
            byte buf[] = new byte[1024];
            InputStream is = pc.getInputStream();
            int ln = is.read ( buf );

            while ( ln >= 0 )
            {
                if ( ln > 0 )
                {
                    System.out.write ( buf, 0, ln );
                }

                ln = is.read ( buf );
            }

            System.out.println ( "EXITING.." );

            try
            {
                pc.waitFor();
            }

            catch ( Exception e )
            {
            }

            return pc.exitValue();
        }

        catch ( IOException e )
        {
            e.printStackTrace();
        }

        return 99;

    }

    public static int[] convertVersionString ( String v )
    {
        if ( v != null )
        {
            Matcher m = Pattern.compile ( "(\\d+)\\.(\\d+)\\.(\\d+)" ).matcher ( v );

            if ( m.find() )
            {
                int va[] = new int[3];
                va[0] = Integer.valueOf ( m.group ( 1 ) );
                va[1] = Integer.valueOf ( m.group ( 2 ) );
                va[2] = Integer.valueOf ( m.group ( 3 ) );
                System.out.println ( "VERSION: " + va[0] + "." + va[1] + "." + va[2] );
                return va;
            }

        }

        return new int[] {0, 0, 0};

    }

    public static int compareVersions ( String ol, String nv )
    {
        int oldv[] = Wrapper.convertVersionString ( ol );
        int newv[] = Wrapper.convertVersionString ( nv );

        for ( int c = 0; c < oldv.length; c++ )
        {
            System.out.println ( "oldv: " + oldv[c] + " newv: " + newv[c] );

            if ( oldv[c] > newv[c] )
            {
                //this means that we have probably upgraded to an older
                //version that what is in the aktie.jar.  so we delete
                //the version file and restart.  so the newer jars in
                //aktie.jar are unzipped again.
                return 1;
            }

            if ( oldv[c] < newv[c] )
            {
                //This is fine.  We have just upgraded.
                return -1;
            }

        }

        return 0;
    }

    /*
        Check if this jar is a newer version of the current files in the
        library
    */
    public static boolean isNewer()
    {
        File vf = new File ( RUNDIR + File.separator + "aktie_node" + File.separator + VERSION_FILE );

        if ( vf.exists() )
        {
            try
            {
                FileReader fr = new FileReader ( vf );
                BufferedReader br = new BufferedReader ( fr );
                String oldstr = br.readLine();
                br.close();

                if ( compareVersions ( oldstr, VERSION ) < 0 )
                {
                    return true;
                }

                return false;
            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        return true;
    }

    public static void deleteLibDir()
    {
        File f = new File ( LIBDIR );

        if ( f.exists() && f.isDirectory() )
        {
            File fl[] = f.listFiles();

            for ( int c = 0; c < fl.length; c++ )
            {
                fl[c].delete();
            }

        }

    }

    public static void unZipIt()
    {

        byte[] buffer = new byte[1024];

        try
        {

            //create output directory is not exists
            File folder = new File ( RUNDIR );

            if ( !folder.exists() )
            {
                folder.mkdir();
            }

            //get the zip file content
            String fn = URLDecoder.decode ( Wrapper.class.getProtectionDomain()
                                            .getCodeSource()
                                            .getLocation()
                                            .getPath(), "UTF-8" );
            File jarfile = new File ( fn );
            ZipInputStream zis =
                new ZipInputStream ( new FileInputStream ( jarfile ) );
            //get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();

            while ( ze != null )
            {

                String fileName = ze.getName();
                File newFile = new File ( RUNDIR + File.separator + fileName );

                System.out.println ( "file unzip : " + newFile.getAbsoluteFile() );

                //create all non exists folders
                //else you will hit FileNotFoundException for compressed folder
                File pd = new File ( newFile.getParent() );
                pd.mkdirs();

                if ( pd.isDirectory() && !ze.isDirectory() )
                {

                    FileOutputStream fos = new FileOutputStream ( newFile );

                    int len;

                    while ( ( len = zis.read ( buffer ) ) > 0 )
                    {
                        fos.write ( buffer, 0, len );
                    }

                    fos.close();
                }

                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();

            System.out.println ( "Done" );

        }

        catch ( IOException ex )
        {
            ex.printStackTrace();
        }

    }

    public static void unZipUpgrade ( File upfile )
    {

        byte[] buffer = new byte[1024];

        try
        {

            //create output directory is not exists
            File folder = new File ( LIBDIR );

            if ( !folder.exists() )
            {
                folder.mkdir();
            }

            //get the zip file content
            ZipInputStream zis =
                new ZipInputStream ( new FileInputStream ( upfile ) );
            //get the zipped file list entry
            ZipEntry ze = zis.getNextEntry();

            while ( ze != null )
            {

                String fileName = ze.getName();
                File newFile = new File ( LIBDIR + File.separator + fileName );

                //If the file name starts with swt* then only copy the file
                //that matches the one already in the dir
                if ( fileName.startsWith ( "swt_" ) )
                {
                    if ( !newFile.exists() )
                    {
                        //SKIP THIS FILE
                        continue;
                    }

                }

                System.out.println ( "file unzip : " + newFile.getAbsoluteFile() );

                //create all non exists folders
                //else you will hit FileNotFoundException for compressed folder
                File pd = new File ( newFile.getParent() );
                pd.mkdirs();

                if ( pd.isDirectory() && !ze.isDirectory() )
                {

                    FileOutputStream fos = new FileOutputStream ( newFile );

                    int len;

                    while ( ( len = zis.read ( buffer ) ) > 0 )
                    {
                        fos.write ( buffer, 0, len );
                    }

                    fos.close();
                }

                if ( "DELETELIST".equals ( fileName ) )
                {
                    BufferedReader br = new BufferedReader ( new FileReader ( newFile ) );
                    String ln = br.readLine();

                    while ( ln != null )
                    {
                        File df = new File ( LIBDIR + File.separator + ln );

                        if ( df.exists() )
                        {
                            if ( !df.delete() )
                            {
                                System.out.println ( "WARNING: COULD NOT REMOVE " + df + " Please do so manually." );
                            }

                        }

                        ln = br.readLine();
                    }

                    br.close();
                    newFile.delete();
                }

                ze = zis.getNextEntry();
            }

            zis.closeEntry();
            zis.close();

            System.out.println ( "Done Upgrading" );

        }

        catch ( IOException ex )
        {
            ex.printStackTrace();
        }

    }

    public static Properties loadExistingProps()
    {
        Properties p = new Properties();
        File propfile = new File ( RUNDIR + File.separator + "aktie_node" + File.separator + "aktie.pros" );

        if ( propfile.exists() )
        {
            try
            {
                FileInputStream fis = new FileInputStream ( propfile );
                p.load ( fis );
                fis.close();
            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        return p;
    }

    public static void savePropsFile ( Properties p )
    {
        File propfile = new File ( RUNDIR + File.separator + "aktie_node" + File.separator + "aktie.pros" );

        try
        {
            FileOutputStream fos = new FileOutputStream ( propfile );
            p.store ( fos, "Aktie properties" );
            fos.close();
        }

        catch ( Exception e )
        {
            e.printStackTrace();
        }

    }

    public static boolean getAutoUpdate()
    {
        boolean update = true;

        Properties p = loadExistingProps();

        String m = p.getProperty ( "aktie.update" );

        if ( m != null )
        {
            try
            {
                update = Boolean.valueOf ( m );
            }

            catch ( Exception e )
            {
            }

        }

        return update;
    }

    public static void saveAutoUpdate ( boolean u )
    {
        Properties p = loadExistingProps();

        p.setProperty ( "aktie.update", Boolean.toString ( u ) );

        savePropsFile ( p );
    }

    public static String getLastDevMessage()
    {
        String msg = "Developer messages.";

        Properties p = loadExistingProps();

        String m = p.getProperty ( "aktie.developerMessage" );

        if ( m != null )
        {
            msg = m;
        }

        return msg;
    }

    public static void saveLastDevMessage ( String msg )
    {
        Properties p = loadExistingProps();

        p.setProperty ( "aktie.developerMessage", msg );

        savePropsFile ( p );
    }

    public static int getStartDestinationDelay()
    {
        int r = 20 * 60; // default of 20 minutes
        Properties p = loadExistingProps();
        String m = p.getProperty ( "aktie.startdestdelay" );

        if ( m != null )
        {
            try
            {
                r = Integer.valueOf ( m );
            }

            catch ( Exception e )
            {
            }

        }

        return r;
    }

    public static void setStartDestinationDelay ( int d )
    {
        Properties p = loadExistingProps();

        p.setProperty ( "aktie.startdestdelay", Integer.toString ( d ) );

        savePropsFile ( p );
    }

    public static boolean getStartDestinationsOnStartup()
    {
        boolean r = true;
        Properties p = loadExistingProps();
        String m = p.getProperty ( "aktie.startdestonstartup" );

        if ( m != null )
        {
            try
            {
                r = Boolean.valueOf ( m );
            }

            catch ( Exception e )
            {
            }

        }

        return r;
    }

    public void saveStartDestinationOnStartup ( boolean s )
    {
        Properties p = loadExistingProps();

        p.setProperty ( "aktie.startdestonstartup", Boolean.toString ( s ) );

        savePropsFile ( p );
    }

    public static String getUpdateLength ( String file )
    {
        Properties p = loadExistingProps();

        return p.getProperty ( "length." + file );
    }

    public static void saveUpdateLength ( String file, String hash )
    {
        Properties p = loadExistingProps();

        p.setProperty ( "length." + file, hash );

        savePropsFile ( p );
    }

    public static boolean getIsDeveloper ( )
    {
        Properties p = loadExistingProps();

        boolean e = false;
        String ep = p.getProperty ( "aktie.developer" );

        if ( "true".equals ( ep ) )
        {
            e = true;
        }

        return e;
    }

    public static boolean getEnabledShareManager ( )
    {
        Properties p = loadExistingProps();

        boolean e = true;
        String ep = p.getProperty ( "aktie.sharemanager.enabled" );

        if ( ep != null && !"true".equals ( ep ) )
        {
            e = false;
        }

        return e;
    }

    public static void saveEnabledShareManager ( boolean e )
    {
        Properties p = loadExistingProps();

        p.setProperty ( "aktie.sharemanager.enabled", Boolean.toString ( e ) );

        savePropsFile ( p );
    }

    public static int getClientPort()
    {
        Properties p = loadExistingProps();

        int rt = 5789;
        String ep = p.getProperty ( "aktie.client.port" );

        if ( ep != null )
        {
            try
            {
                rt = Integer.valueOf ( ep );
            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        return rt;
    }

    public static void saveClientPort ( int rt )
    {
        Properties p = loadExistingProps();

        p.setProperty ( "aktie.client.port", Integer.toString ( rt ) );

        savePropsFile ( p );
    }

    public static String getClientInterface()
    {
        String msg = "0.0.0.0";

        Properties p = loadExistingProps();

        String m = p.getProperty ( "aktie.client.interface" );

        if ( m != null )
        {
            msg = m;
        }

        return msg;
    }

    public static void saveClientInterface ( String msg )
    {
        Properties p = loadExistingProps();

        p.setProperty ( "aktie.client.interface", msg );

        savePropsFile ( p );
    }

    public static int getPaymentRank()
    {
        Properties p = loadExistingProps();

        int rt = 5;
        String ep = p.getProperty ( "aktie.spam.rank" );

        if ( ep != null )
        {
            try
            {
                rt = Integer.valueOf ( ep );
            }

            catch ( Exception e )
            {
                e.printStackTrace();
            }

        }

        return rt;
    }

    public static void savePaymentRank ( int rt )
    {
        Properties p = loadExistingProps();

        p.setProperty ( "aktie.spam.rank", Integer.toString ( rt ) );

        savePropsFile ( p );
    }

    public static long getCheckPayment()
    {
        long pm = OLDPAYMENT;
        long today = System.currentTimeMillis();

        if ( today >= CHECKNEWPAYMENTAFTER )
        {
            pm = NEWPAYMENT;
        }

        return pm;
    }

    public static long getGenPayment()
    {
        return NEWPAYMENT;
    }

}
