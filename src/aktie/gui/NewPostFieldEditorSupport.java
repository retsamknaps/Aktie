package aktie.gui;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;

import aktie.data.CObj;

import org.eclipse.jface.viewers.CheckboxCellEditor;

//import org.eclipse.swt.SWT;

//import aktie.data.CObj;

public class NewPostFieldEditorSupport extends EditingSupport
{

    private TextCellEditor text;
    private NumValidator numValidator;
    private TextCellEditor textNum;
    private DecValidator decValidator;
    private TextCellEditor textDec;
    private CheckboxCellEditor checkbox;
    private TableViewer tviewer;

    private class NumValidator implements ICellEditorValidator
    {
        public long min;
        public long max;
        @Override
        public String isValid ( Object arg0 )
        {
            String r = null;
            String ts = ( String ) arg0;

            try
            {
                long v = Long.valueOf ( ts );

                if ( v < min )
                {
                    r = "The minimum value is " + min;
                }

                if ( v > max )
                {
                    r = "The maximum value is " + max;
                }

            }

            catch ( Exception e )
            {
                r = "Must be a whole number.";
            }

            return r;
        }

    }

    private class DecValidator implements ICellEditorValidator
    {
        public double min;
        public double max;
        @Override
        public String isValid ( Object arg0 )
        {
            String r = null;
            String ts = ( String ) arg0;

            try
            {
                double v = Double.valueOf ( ts );

                if ( v < min )
                {
                    r = "The minimum value is " + min;
                }

                if ( v > max )
                {
                    r = "The maximum value is " + max;
                }

            }

            catch ( Exception e )
            {
                r = "Must be a whole number.";
            }

            return r;
        }

    }

    public NewPostFieldEditorSupport ( ColumnViewer viewer )
    {
        super ( viewer );
        tviewer = ( TableViewer ) viewer;
        text = new TextCellEditor ( tviewer.getTable() );
        checkbox = new CheckboxCellEditor ( tviewer.getTable() );
        textNum = new TextCellEditor ( tviewer.getTable() );
        numValidator = new NumValidator();
        textNum.setValidator ( numValidator );
        textDec = new TextCellEditor ( tviewer.getTable() );
        decValidator = new DecValidator();
        textDec.setValidator ( decValidator );
    }

    @Override
    protected boolean canEdit ( Object c )
    {
        if ( c instanceof CObjListGetter )
        {
            CObjListGetter gt = ( CObjListGetter ) c;
            CObj co = gt.getCObj();
            String fdtyp = co.getString ( CObj.FLD_TYPE );

            if ( CObj.FLD_TYPE_STRING.equals ( fdtyp ) )
            {
                return true;
            }

            if ( CObj.FLD_TYPE_BOOL.equals ( fdtyp ) )
            {
                return true;
            }

            if ( CObj.FLD_TYPE_NUMBER.equals ( fdtyp ) )
            {
                return true;
            }

            if ( CObj.FLD_TYPE_DECIMAL.equals ( fdtyp ) )
            {
                return true;
            }

        }

        return false;
    }

    @Override
    protected CellEditor getCellEditor ( Object c )
    {
        if ( c instanceof CObjListGetter )
        {
            CObjListGetter gt = ( CObjListGetter ) c;
            CObj co = gt.getCObj();
            String fdtyp = co.getString ( CObj.FLD_TYPE );

            if ( CObj.FLD_TYPE_STRING.equals ( fdtyp ) )
            {
                return text;
            }

            if ( CObj.FLD_TYPE_BOOL.equals ( fdtyp ) )
            {
                return checkbox;
            }

            if ( CObj.FLD_TYPE_NUMBER.equals ( fdtyp ) )
            {
                numValidator.max = Long.MAX_VALUE;
                numValidator.min = Long.MIN_VALUE;
                Long m = co.getNumber ( CObj.FLD_MAX );

                if ( m != null )
                {
                    numValidator.max = m;
                }

                m = co.getNumber ( CObj.FLD_MIN );

                if ( m != null )
                {
                    numValidator.min = m;
                }

                return textNum;
            }

            if ( CObj.FLD_TYPE_DECIMAL.equals ( fdtyp ) )
            {
                decValidator.max = Double.MAX_VALUE;
                decValidator.min = Double.MIN_VALUE;
                Double m = co.getDecimal ( CObj.FLD_MAX );

                if ( m != null )
                {
                    decValidator.max = m;
                }

                m = co.getDecimal ( CObj.FLD_MIN );

                if ( m != null )
                {
                    decValidator.min = m;
                }

                return textDec;
            }

        }

        return null;
    }

    @Override
    protected Object getValue ( Object c )
    {
        if ( c instanceof CObjListGetter )
        {
            CObjListGetter gt = ( CObjListGetter ) c;
            CObj co = gt.getCObj();
            String fdtyp = co.getString ( CObj.FLD_TYPE );

            if ( CObj.FLD_TYPE_STRING.equals ( fdtyp ) )
            {
                String v = co.getPrivate ( CObj.FLD_VAL );

                if ( v == null ) { v = ""; }

                return v;
            }

            if ( CObj.FLD_TYPE_BOOL.equals ( fdtyp ) )
            {
                String v = co.getPrivate ( CObj.FLD_VAL );
                return "true".equals ( v );
            }

            if ( CObj.FLD_TYPE_NUMBER.equals ( fdtyp ) )
            {
                String v = co.getPrivate ( CObj.FLD_VAL );

                if ( v == null )
                {
                    Long mn = co.getNumber ( CObj.FLD_MIN );

                    if ( mn != null )
                    {
                        v = mn.toString();
                    }

                    else
                    {
                        mn = co.getNumber ( CObj.FLD_MAX );

                        if ( mn != null )
                        {
                            v = mn.toString();
                        }

                        else
                        {
                            v = "0";
                        }

                    }

                }

                return v;
            }

            if ( CObj.FLD_TYPE_DECIMAL.equals ( fdtyp ) )
            {
                String v = co.getPrivate ( CObj.FLD_VAL );

                if ( v == null )
                {
                    Double mn = co.getDecimal ( CObj.FLD_MIN );

                    if ( mn != null )
                    {
                        v = mn.toString();
                    }

                    else
                    {
                        mn = co.getDecimal ( CObj.FLD_MAX );

                        if ( mn != null )
                        {
                            v = mn.toString();
                        }

                        else
                        {
                            v = "0";
                        }

                    }

                }

                return v;
            }

        }

        return "";
    }

    @Override
    protected void setValue ( Object c, Object v )
    {
        if ( c instanceof CObjListGetter )
        {
            CObjListGetter gt = ( CObjListGetter ) c;
            CObj co = gt.getCObj();
            co.pushPrivate ( CObj.FLD_VAL, v.toString() );
            tviewer.refresh();
        }

    }

}
