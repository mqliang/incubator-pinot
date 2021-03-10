package org.apache.pinot.core.operator.combine;

import org.apache.pinot.core.common.DataSourceMetadata;
import org.apache.pinot.core.operator.query.SelectionOrderByOperator;



  public class MinMaxValueContext {
    final SelectionOrderByOperator _operator;
    final Comparable _minValue;
    final Comparable _maxValue;

    MinMaxValueContext(SelectionOrderByOperator operator, String column) {
      _operator = operator;
      DataSourceMetadata dataSourceMetadata = operator.getIndexSegment().getDataSource(column).getDataSourceMetadata();
      _minValue = dataSourceMetadata.getMinValue();
      _maxValue = dataSourceMetadata.getMaxValue();
    }
  }
