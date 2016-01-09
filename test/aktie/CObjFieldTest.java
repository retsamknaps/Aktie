package aktie;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import aktie.data.CObj;

public class CObjFieldTest
{

    @Test
    public void testStringField()
    {
        CObj fld = new CObj();
        fld.pushString ( CObj.FLD_TYPE, CObj.FLD_TYPE_STRING );
        fld.pushString ( CObj.FLD_NAME, "Test field" );
        fld.pushString ( CObj.FLD_DESC, "This is a test field it is for testing" );
        fld.simpleDigest();

        CObj fld2 = new CObj();
        fld2.pushString ( CObj.FLD_TYPE, CObj.FLD_TYPE_BOOL );
        fld2.pushString ( CObj.FLD_NAME, "Test bool" );
        fld2.pushString ( CObj.FLD_DESC, "This is a test bool" );
        fld2.simpleDigest();

        CObj fld3 = new CObj();
        fld3.pushString ( CObj.FLD_TYPE, CObj.FLD_TYPE_DECIMAL );
        fld3.pushString ( CObj.FLD_NAME, "Test dec" );
        fld3.pushString ( CObj.FLD_DESC, "This is a test dec" );
        fld3.pushDecimal ( CObj.FLD_MIN, 0.001D );
        fld3.pushDecimal ( CObj.FLD_MAX, 100.03D );
        fld3.simpleDigest();

        CObj fld4 = new CObj();
        fld4.pushString ( CObj.FLD_TYPE, CObj.FLD_TYPE_NUMBER );
        fld4.pushString ( CObj.FLD_NAME, "Test num" );
        fld4.pushString ( CObj.FLD_DESC, "This is a test num" );
        fld4.pushNumber ( CObj.FLD_MIN, -3L );
        fld4.pushNumber ( CObj.FLD_MAX, 200L );
        fld4.simpleDigest();

        CObj fld5 = new CObj();
        fld5.pushString ( CObj.FLD_TYPE, CObj.FLD_TYPE_OPT );
        fld5.pushString ( CObj.FLD_NAME, "Test opt" );
        fld5.pushString ( CObj.FLD_DESC, "This is a test opt" );
        fld5.pushString ( CObj.FLD_VAL + "_0", "value00" );
        fld5.pushString ( CObj.FLD_VAL + "_1", "value01" );
        fld5.pushString ( CObj.FLD_VAL + "_2", "value02" );
        fld5.pushString ( CObj.FLD_DEF, "value01" );
        fld5.simpleDigest();

        CObj fld6 = new CObj();
        fld6.pushString ( CObj.FLD_TYPE, CObj.FLD_TYPE_TEXT );
        fld6.pushString ( CObj.FLD_NAME, "Test text" );
        fld6.pushString ( CObj.FLD_DESC, "This is a test text" );
        fld6.simpleDigest();

        CObj fld7 = new CObj();
        fld7.pushString ( CObj.FLD_TYPE, CObj.FLD_TYPE_DECIMAL );
        fld7.pushString ( CObj.FLD_NAME, "Not used" );
        fld7.pushString ( CObj.FLD_DESC, "This is not used" );
        fld7.simpleDigest();

        CObj pst = new CObj();
        pst.pushString ( CObj.SUBJECT, "here is the subject" );
        pst.pushString ( CObj.BODY, "This is the body" );
        pst.pushString ( CObj.CREATOR, "0123456789abcd" );
        pst.pushPrivateNumber ( CObj.PRV_USER_RANK, 4L );

        pst.setNewFieldString ( fld, "String field" );
        pst.setNewFieldBool ( fld2, true );
        pst.setNewFieldDecimal ( fld3, 1.3046D );
        pst.setNewFieldNumber ( fld4, 12L );
        pst.setNewFieldString ( fld5, "value02" );
        pst.setNewFieldText ( fld6, "some text" );
        pst.setFieldDecimal ( fld7.getDig(), 2.98D );

        String v = pst.getFieldString ( fld.getDig() );
        assertEquals ( "String field", v );
        String t = pst.getFieldType ( fld.getDig() );
        assertEquals ( CObj.FLD_TYPE_STRING, t );
        String n = pst.getFieldName ( fld.getDig() );
        assertEquals ( "Test field", n );
        String d = pst.getFieldDesc ( fld.getDig() );
        assertEquals ( "This is a test field it is for testing", d );

        boolean bv = pst.getFieldBoolean ( fld2.getDig() );
        assertTrue ( bv );
        t = pst.getFieldType ( fld2.getDig() );
        assertEquals ( CObj.FLD_TYPE_BOOL, t );
        n = pst.getFieldName ( fld2.getDig() );
        assertEquals ( "Test bool", n );
        d = pst.getFieldDesc ( fld2.getDig() );
        assertEquals ( "This is a test bool", d );

        Double dv = pst.getFieldDecimal ( fld3.getDig() );
        assertEquals ( 1.3046D, dv.doubleValue(), 0D );
        t = pst.getFieldType ( fld3.getDig() );
        assertEquals ( CObj.FLD_TYPE_DECIMAL, t );
        n = pst.getFieldName ( fld3.getDig() );
        assertEquals ( "Test dec", n );
        d = pst.getFieldDesc ( fld3.getDig() );
        assertEquals ( "This is a test dec", d );
        Double maxv = pst.getFieldDecimalMax ( fld3.getDig() );
        assertEquals ( 100.03D, maxv.doubleValue(), 0D );
        Double minv = pst.getFieldDecimalMin ( fld3.getDig() );
        assertEquals ( 0.001D, minv.doubleValue(), 0D );

        Long lv = pst.getFieldNumber ( fld4.getDig() );
        assertEquals ( 12L, lv.longValue() );
        t = pst.getFieldName ( fld4.getDig() );
        assertEquals ( "Test num", t );
        d = pst.getFieldDesc ( fld4.getDig() );
        assertEquals ( "This is a test num", d );
        Long maxl = pst.getFieldNumberMax ( fld4.getDig() );
        assertEquals ( 200L, maxl.longValue() );
        Long minl = pst.getFieldNumberMin ( fld4.getDig() );
        assertEquals ( -3L, minl.longValue() );

        v = pst.getFieldString ( fld5.getDig() );
        assertEquals ( "value02", v );
        t = pst.getFieldType ( fld5.getDig() );
        assertEquals ( CObj.FLD_TYPE_OPT, t );
        n = pst.getFieldName ( fld5.getDig() );
        assertEquals ( "Test opt", n );
        d = pst.getFieldDesc ( fld5.getDig() );
        assertEquals ( "This is a test opt", d );
        Set<String> ov = pst.getFieldOptVals ( fld5.getDig() );
        assertEquals ( 3, ov.size() );
        assertTrue ( ov.contains ( "value00" ) );
        assertTrue ( ov.contains ( "value01" ) );
        assertTrue ( ov.contains ( "value02" ) );
        String df = pst.getFieldStringDef ( fld5.getDig() );
        assertEquals ( "value01", df );

        v = pst.getFieldText ( fld6.getDig() );
        assertEquals ( "some text", v );
        t = pst.getFieldType ( fld6.getDig() );
        assertEquals ( CObj.FLD_TYPE_TEXT, t );
        n = pst.getFieldName ( fld6.getDig() );
        assertEquals ( "Test text", n );
        d = pst.getFieldDesc ( fld6.getDig() );
        assertEquals ( "This is a test text", d );

        dv = pst.getFieldDecimal ( fld7.getDig() );
        assertEquals ( 2.98D, dv, 0D );

        Set<String> fl = pst.listFields();
        assertEquals ( 7, fl.size() );
        assertTrue ( fl.contains ( fld.getDig() ) );
        assertTrue ( fl.contains ( fld2.getDig() ) );
        assertTrue ( fl.contains ( fld3.getDig() ) );
        assertTrue ( fl.contains ( fld4.getDig() ) );
        assertTrue ( fl.contains ( fld5.getDig() ) );
        assertTrue ( fl.contains ( fld6.getDig() ) );
        assertTrue ( fl.contains ( fld7.getDig() ) );

        List<CObj> nl = pst.listNewFields();
        assertEquals ( 6, nl.size() );
        boolean fnd1 = false;
        boolean fnd2 = false;
        boolean fnd3 = false;
        boolean fnd4 = false;
        boolean fnd5 = false;
        boolean fnd6 = false;

        for ( CObj c : nl )
        {
            int ne = 0;

            Long r = c.getPrivateNumber ( CObj.PRV_USER_RANK );
            assertEquals ( 4L, r.longValue() );
            String cr = c.getPrivate ( CObj.CREATOR );
            assertEquals ( "0123456789abcd", cr );

            if ( fld.whoopyPubEquals ( c ) )
            {
                ne++;
                fnd1 = true;
            }

            if ( fld2.whoopyPubEquals ( c ) )
            {
                ne++;
                fnd2 = true;
            }

            if ( fld3.whoopyPubEquals ( c ) )
            {
                ne++;
                fnd3 = true;
            }

            if ( fld4.whoopyPubEquals ( c ) )
            {
                ne++;
                fnd4 = true;
            }

            if ( fld5.whoopyPubEquals ( c ) )
            {
                ne++;
                fnd5 = true;
            }

            if ( fld6.whoopyPubEquals ( c ) )
            {
                ne++;
                fnd6 = true;
            }

            assertEquals ( 1, ne );
        }

        assertTrue ( fnd1 );
        assertTrue ( fnd2 );
        assertTrue ( fnd3 );
        assertTrue ( fnd4 );
        assertTrue ( fnd5 );
        assertTrue ( fnd6 );

    }

}
