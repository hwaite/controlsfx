/**
 * Copyright (c) 2013, ControlsFX
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of ControlsFX, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL CONTROLSFX BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package impl.org.controlsfx.spreadsheet;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TableFocusModel;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.Region;
import javafx.util.Callback;

import org.controlsfx.control.spreadsheet.SpreadsheetCell;
import org.controlsfx.control.spreadsheet.SpreadsheetColumn;
import org.controlsfx.control.spreadsheet.SpreadsheetView;

import com.sun.javafx.scene.control.skin.TableHeaderRow;
import com.sun.javafx.scene.control.skin.TableViewSkin;
import com.sun.javafx.scene.control.skin.VirtualFlow;
import com.sun.javafx.scene.control.skin.VirtualScrollBar;

/**
 * This skin is actually the skin of the SpreadsheetGridView (tableView) contained
 * within the SpreadsheetView. The skin for the SpreadsheetView itself currently
 * resides inside the SpreadsheetView constructor!
 * 
 */
public class GridViewSkin extends TableViewSkin<ObservableList<SpreadsheetCell>> {
    /** Default height of a row. */
    public static final double DEFAULT_CELL_HEIGHT;
    
    static final double DATE_CELL_MIN_WIDTH = 105;
    static {
        double cell_size = 24.0;
        try {
            Class<?> clazz = com.sun.javafx.scene.control.skin.CellSkinBase.class;
            Field f = clazz.getDeclaredField("DEFAULT_CELL_SIZE");
            f.setAccessible(true);
            cell_size = f.getDouble(null);
        } catch (NoSuchFieldException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        DEFAULT_CELL_HEIGHT = cell_size;
    }
    
    /** Default with of the VerticalHeader. */
    protected final double DEFAULT_VERTICALHEADER_WIDTH = 40.0;
    
    private DoubleProperty verticalHeaderWidth = new SimpleDoubleProperty(DEFAULT_VERTICALHEADER_WIDTH);
    
    public DoubleProperty verticalHeaderWidthProperty() {
        return verticalHeaderWidth;
    }
    
    public void setVerticalHeaderWidth(double width){
    	verticalHeaderWidth.set(width);
    }
    
    public double getVerticalHeaderWidth(){
    	return verticalHeaderWidth.get();
    }

    /** The editor. */
    private GridCellEditor spreadsheetCellEditorImpl;
    
    protected final SpreadsheetHandle handle;
    protected SpreadsheetView spreadsheetView;
    protected VerticalHeader verticalHeader;
    
    /**
     * The height of our fixedRows
     */
    private double fixedRowHeight = 0;

    public double getFixedRowHeight(){
    	return fixedRowHeight;
    }
    
    public GridViewSkin(final SpreadsheetHandle handle) {
        super(handle.getGridView());
        this.handle = handle;
        this.spreadsheetView = handle.getView();
        spreadsheetCellEditorImpl = new GridCellEditor(handle);
        TableView<ObservableList<SpreadsheetCell>> tableView = handle.getGridView();
        tableView.setEditable(true);

        // Do nothing basically but give access to the Hover Property.
        tableView.setRowFactory(new Callback<TableView<ObservableList<SpreadsheetCell>>, TableRow<ObservableList<SpreadsheetCell>>>() {
            @Override public TableRow<ObservableList<SpreadsheetCell>> call(TableView<ObservableList<SpreadsheetCell>> p) {
                return new GridRow(handle);
            }
        });

        tableView.getStyleClass().add("cell-spreadsheet");

        spreadsheetView.getFixedRows().addListener(fixedRowsListener);
        spreadsheetView.getFixedColumns().addListener(fixedColumnsListener);
       
        init();
        //Because fixedRow Listener is not reacting first time.
        computeFixedRowHeight();
    }
    
	protected void init() {
        getFlow().getVerticalBar().valueProperty()
                .addListener(vbarValueListener);
        verticalHeader = new VerticalHeader(handle, verticalHeaderWidth);
        getChildren().addAll(verticalHeader);

        ((HorizontalHeader) getTableHeaderRow()).init();
        verticalHeader.init(this, (HorizontalHeader) getTableHeaderRow());
        
        getFlow().init(spreadsheetView);
    }
	
    @Override
    protected void layoutChildren(double x, double y, double w, final double h) {
        if (spreadsheetView == null) { return; }
        if (spreadsheetView.showRowHeaderProperty().get()) {
            x += getVerticalHeaderWidth();
            w -= getVerticalHeaderWidth();
        }

        super.layoutChildren(x, y, w, h);

        final double baselineOffset = getSkinnable().getLayoutBounds()
                .getHeight() / 2;
        double tableHeaderRowHeight = 0;

        if (spreadsheetView.showColumnHeaderProperty().get()) {
            // position the table header
            tableHeaderRowHeight = getTableHeaderRow().prefHeight(-1);
            layoutInArea(getTableHeaderRow(), x, y, w, tableHeaderRowHeight,
                    baselineOffset, HPos.CENTER, VPos.CENTER);

            y += tableHeaderRowHeight;
        } else {
        	// This is temporary handled in the HorizontalHeader with Css
        	// FIXME tweak open in RT-32673
        }

        if (spreadsheetView.showRowHeaderProperty().get()) {
            layoutInArea(verticalHeader, x - getVerticalHeaderWidth(), y
                    - tableHeaderRowHeight, w, h, baselineOffset, HPos.CENTER,
                    VPos.CENTER);
        }
    }
    
    /**
     * When the vertical moves, we update the verticalHeader
     */
    final InvalidationListener vbarValueListener = new InvalidationListener() {
        @Override
        public void invalidated(Observable valueModel) {
            verticalScroll();
        }
    };

    protected void verticalScroll() {
        verticalHeader.updateScrollY();
    }

    @Override
    protected void onFocusPreviousCell() {
        final TableFocusModel<?, ?> fm = getFocusModel();
        if (fm == null) { return; }
        /*****************************************************************
         * MODIFIED BY NELLARMONIA
         *****************************************************************/
        final int row = fm.getFocusedIndex();
        // We try to make visible the rows that may be hiden by Fixed rows
        if (!getFlow().getCells().isEmpty()
                && getFlow().getCells().get(spreadsheetView.getFixedRows().size())
                        .getIndex() > row
                && !spreadsheetView.getFixedRows().contains(row)) {
            flow.scrollTo(row);
        } else {
            flow.show(row);
        }
        scrollHorizontally();
        /*****************************************************************
         * END OF MODIFIED BY NELLARMONIA
         *****************************************************************/
    }

    @Override
    protected void onFocusNextCell() {
        final TableFocusModel<?, ?> fm = getFocusModel();
        if (fm == null) { return; }
        /*****************************************************************
         * MODIFIED BY NELLARMONIA
         *****************************************************************/
        final int row = fm.getFocusedIndex();
        //FIXME This is not true anymore I think
        // We try to make visible the rows that may be hidden by Fixed rows
        if (!getFlow().getCells().isEmpty()
                && getFlow().getCells().get(spreadsheetView.getFixedRows().size())
                        .getIndex() > row
                && !spreadsheetView.getFixedRows().contains(row)) {
            flow.scrollTo(row);
        } else {
            flow.show(row);
        }
        scrollHorizontally();
        /*****************************************************************
         * END OF MODIFIED BY NELLARMONIA
         *****************************************************************/
    }
    @Override
    protected void onSelectPreviousCell() {
        super.onSelectPreviousCell();
        scrollHorizontally();
    }

    @Override
    protected void onSelectNextCell() {
        super.onSelectNextCell();
        scrollHorizontally();
    }
    
    /**
     * We listen on the FixedRows in order to do the modification in the
     * VirtualFlow
     */
    private final ListChangeListener<Integer> fixedRowsListener = new ListChangeListener<Integer>() {
        @Override
        public void onChanged(Change<? extends Integer> c) {
        	computeFixedRowHeight();
        	
            // requestLayout() not responding immediately..
            getFlow().layoutTotal();
        }

    };

    /**
     * We compute the total height of the fixedRows so that 
     * the selection can use it without performance regression.
     */
    private void computeFixedRowHeight(){
    	fixedRowHeight = 0;
		for(int i: spreadsheetView.getFixedRows()){
			fixedRowHeight+= spreadsheetView.getGrid().getRowHeight(i);
		}
    }
    /**
     * We listen on the FixedColumns in order to do the modification in the
     * VirtualFlow
     */
    private final ListChangeListener<SpreadsheetColumn<?>> fixedColumnsListener = new ListChangeListener<SpreadsheetColumn<?>>() {
        @Override
        public void onChanged(Change<? extends SpreadsheetColumn<?>> c) {
            if (spreadsheetView.getFixedColumns().size() > c.getList().size()) {
                for (int i = 0; i < getFlow().getCells().size(); ++i) {
                    ((GridRow) getFlow().getCells().get(i))
                            .putFixedColumnToBack();
                }
            }

            // requestLayout() not responding immediately..
            getFlow().layoutTotal();
        }

    };

    @Override
    protected VirtualFlow<TableRow<ObservableList<SpreadsheetCell>>> createVirtualFlow() {
        return new GridVirtualFlow<TableRow<ObservableList<SpreadsheetCell>>>(this);
    }

    protected TableHeaderRow createTableHeaderRow() {
        return new HorizontalHeader(this);
    }

    BooleanProperty getTableMenuButtonVisibleProperty() {
        return tableMenuButtonVisibleProperty();
    }

    @Override
    protected void scrollHorizontally(TableColumn<ObservableList<SpreadsheetCell>, ?> col) {

        if (col == null || !col.isVisible()) { return; }

        // work out where this column header is, and it's width (start -> end)
        double start = 0;// scrollX;
        for (TableColumnBase<?, ?> c : getVisibleLeafColumns()) {
            if (c.equals(col)) break;
            start += c.getWidth();
        }

        /*****************************************************************
         * MODIFIED BY NELLARMONIA We modified this function so that we ensure
         * that any selected cells will not be below a fixed column. Because
         * when there's some fixed columns, the "left border" is not the table
         * anymore, but the right side of the last fixed columns.
         *****************************************************************/
        // We add the fixed columns width
        final double fixedColumnWidth = getFixedColumnWidth();

        /*****************************************************************
         * END OF MODIFIED BY NELLARMONIA
         *****************************************************************/
        final double end = start + col.getWidth();

        // determine the visible width of the table
        final double headerWidth = getSkinnable().getWidth()
                - snappedLeftInset() - snappedRightInset();

        // determine by how much we need to translate the table to ensure that
        // the start position of this column lines up with the left edge of the
        // tableview, and also that the columns don't become detached from the
        // right edge of the table
        final double pos = getFlow().getHorizontalBar().getValue();
        final double max = getFlow().getHorizontalBar().getMax();
        double newPos;

        /*****************************************************************
         * MODIFIED BY NELLARMONIA
         *****************************************************************/
        if (start < pos + fixedColumnWidth && start >= 0
                && start >= fixedColumnWidth) {
            newPos = start - fixedColumnWidth < 0 ? start : start
                    - fixedColumnWidth;
        } else {
            final double delta = start < 0 || end > headerWidth ? start - pos
                    - fixedColumnWidth : 0;
            newPos = pos + delta > max ? max : pos + delta;
        }

        /*****************************************************************
         * END OF MODIFIED BY NELLARMONIA
         *****************************************************************/

        // FIXME we should add API in VirtualFlow so we don't end up going
        // direct to the hbar.
        // actually shift the flow - this will result in the header moving
        // as well
        getFlow().getHorizontalBar().setValue(newPos);
    }

    /**
     * Compute the width of the fixed columns in order not to select cells that are
     * hidden by the fixed columns
     * 
     * @return
     */
    private double getFixedColumnWidth() {
        double fixedColumnWidth = 0;
        if (!spreadsheetView.getFixedColumns().isEmpty()) {
            for (int i = 0, max = spreadsheetView.getFixedColumns().size(); i < max; ++i) {
                final TableColumnBase<ObservableList<SpreadsheetCell>, ?> c = getVisibleLeafColumn(i);
                fixedColumnWidth += c.getWidth();
            }
        }
        return fixedColumnWidth;
    }

    public GridVirtualFlow<?> getFlow() {
        return (GridVirtualFlow<?>) flow;
    }

    public GridRow getRow(int index) {
        return (GridRow) getFlow().getCells().get(index);
    }
    
    public int getCellsSize() {
        return getFlow().getCells().size();
    }
    
    public VirtualScrollBar getHBar() {
        return getFlow().getHorizontalBar();
    }
    
    public VirtualScrollBar getVBar() {
        return getFlow().getVerticalBar();
    }

    /**
     * A list of Integer with the current selected Rows. This is useful for HorizontalHeader and
     * VerticalHeader because they need to highlight when a selection is made.
     */
    private final ObservableList<Integer> selectedRows = FXCollections.observableArrayList();
    public ObservableList<Integer> getSelectedRows() {
        return selectedRows;
    }

    /**
     * A list of Integer with the current selected Columns. This is useful for HorizontalHeader and
     * VerticalHeader because they need to highlight when a selection is made.
     */
    private final ObservableList<Integer> selectedColumns= FXCollections.observableArrayList();
    public ObservableList<Integer> getSelectedColumns() {
        return selectedColumns;
    }

	public GridCellEditor getSpreadsheetCellEditorImpl() {
		return spreadsheetCellEditorImpl;
	}
	
	/**
	 * Used in the HorizontalColumnHeader when we need to resize
	 * in double click.
	 * Keep in mind that resize is broken RT-31653
	 * @param tc
	 */
	void resize(TableColumnBase<?, ?> tc){
		resizeColumnToFitContent(getColumns().get(getColumns().indexOf(tc)), -1);
	}

	/**
	 * We want to have extra space when displaying LocalDate because they will
	 * use an editor that display a little icon on the right. Thus, that icon
	 * is reducing the visibility of the date string.
	 */
	@Override public void resizeColumnToFitContent(TableColumn<ObservableList<SpreadsheetCell>, ?> tc, int maxRows) {
        final TableColumn<ObservableList<SpreadsheetCell>, ?> col = tc;
        List<?> items = itemsProperty().get();
        if (items == null || items.isEmpty()) return;
    
        Callback/*<TableColumn<T, ?>, TableCell<T,?>>*/ cellFactory = col.getCellFactory();
        if (cellFactory == null) return;
    
        TableCell<ObservableList<SpreadsheetCell>,?> cell = (TableCell<ObservableList<SpreadsheetCell>, ?>) cellFactory.call(col);
        if (cell == null) return;
        
        // set this property to tell the TableCell we want to know its actual
        // preferred width, not the width of the associated TableColumnBase
        cell.getProperties().put("deferToParentPrefWidth", Boolean.TRUE);
        
        // determine cell padding
        double padding = 10;
        Node n = cell.getSkin() == null ? null : cell.getSkin().getNode();
        if (n instanceof Region) {
            Region r = (Region) n;
            padding = r.snappedLeftInset() + r.snappedRightInset();
        } 
        
        int rows = maxRows == -1 ? items.size() : Math.min(items.size(), maxRows);
        double maxWidth = 0;
        boolean datePresent = false;
        boolean listPresent = false;
        for (int row = 0; row < rows; row++) {
            cell.updateTableColumn(col);
            cell.updateTableView(handle.getGridView());
            cell.updateIndex(row);
            
            if ((cell.getText() != null && !cell.getText().isEmpty()) || cell.getGraphic() != null) {
                getChildren().add(cell);
                
                if(((SpreadsheetCell)cell.getItem()).getItem() instanceof LocalDate){
                	datePresent = true;
                } 
                cell.impl_processCSS(false);
                maxWidth = Math.max(maxWidth, cell.prefWidth(-1));
                getChildren().remove(cell);
            }
        }

        // dispose of the cell to prevent it retaining listeners (see RT-31015)
        cell.updateIndex(-1);
        
        // RT-23486
        double widthMax = maxWidth + padding;
        if(handle.getGridView().getColumnResizePolicy() == TableView.CONSTRAINED_RESIZE_POLICY) {
             widthMax = Math.max(widthMax, col.getWidth());
        }
        
        if(datePresent && widthMax < DATE_CELL_MIN_WIDTH){
        	widthMax = DATE_CELL_MIN_WIDTH;
        }
        col.impl_setWidth(widthMax); 
    }
}
