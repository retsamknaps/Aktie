package aktie.gui;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;

import aktie.data.CObj;

public class AdvSearchMaxEditorSupport extends EditingSupport
{

    private FieldNumValidator numValidator;
    private TextCellEditor textNum;
    private FieldDecValidator decValidator;
    private TextCellEditor textDec;
    private TableViewer tviewer;

    public AdvSearchMaxEditorSupport ( ColumnViewer viewer )
    {
        super ( viewer );
        tviewer = ( TableViewer ) viewer;
        textNum = new TextCellEditor ( tviewer.getTable() );
        numValidator = new FieldNumValidator();
        textNum.setValidator ( numValidator );
        textDec = new TextCellEditor ( tviewer.getTable() );
        decValidator = new FieldDecValidator();
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

            if ( CObj.FLD_TYPE_NUMBER.equals ( fdtyp ) )
            {
                String v = co.getPrivate ( CObj.FLD_MAX );

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
                String v = co.getPrivate ( CObj.FLD_MAX );

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
            co.pushPrivate ( CObj.FLD_MAX, v.toString() );
            tviewer.refresh();
        }

    }

}
