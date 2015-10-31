package aktie.data;

import java.io.File;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

public class HH2Session
{

    private SessionFactory concreteSessionFactory;

    public void init ( String dir )
    {
        try
        {
            Logger.getLogger ( "org.hibernate" ).setLevel ( Level.WARNING );
            Properties p = new Properties ( System.getProperties() );
            p.put ( "com.mchange.v2.log.MLog", "com.mchange.v2.log.FallbackMLog" );
            p.put ( "com.mchange.v2.log.FallbackMLog.DEFAULT_CUTOFF_LEVEL", "WARNING" ); // or any other
            System.setProperties ( p );

            File tmpdir = null;

            Configuration configuration = new Configuration();
            tmpdir = new File ( dir );

            configuration.setProperty ( "hibernate.dialect", "org.hibernate.dialect.H2Dialect" );
            configuration.setProperty ( "hibernate.connection.url", "jdbc:h2:" + tmpdir.getCanonicalPath() + File.separator + "data" );
            configuration.setProperty ( "hibernate.connection.username", "root" );
            configuration.setProperty ( "hibernate.connection.password", "" );
            configuration.setProperty ( "hibernate.hbm2ddl.auto", "update" );
            configuration.setProperty ( "dialect", "org.hibernate.dialect.H2Dialect" );
            configuration.setProperty ( "hibernate.connection.provider_class", "org.hibernate.connection.C3P0ConnectionProvider" );
            configuration.setProperty ( "hibernate.connection.driver_class", "org.h2.Driver" );
            configuration.setProperty ( "hibernate.c3p0.maxPoolSize", "10" );

            //--- Add Mappings ---
            configuration.addAnnotatedClass ( IdentityData.class );
            configuration.addAnnotatedClass ( RequestFile.class );
            configuration.addAnnotatedClass ( RequestIdentities.class );
            configuration.addAnnotatedClass ( CommunityMember.class );
            configuration.addAnnotatedClass ( CommunityMyMember.class );
            configuration.addAnnotatedClass ( DirectoryShare.class );
            configuration.buildMapping();

            StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder().
            applySettings ( configuration.getProperties() );
            concreteSessionFactory = configuration.buildSessionFactory ( builder.build() );

        }

        catch ( Throwable ex )
        {
            ex.printStackTrace();
            throw new ExceptionInInitializerError ( ex );
        }

    }

    public Session getSession() throws HibernateException
    {
        return concreteSessionFactory.openSession();
    }


}
