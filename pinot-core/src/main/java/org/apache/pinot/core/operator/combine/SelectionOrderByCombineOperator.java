package org.apache.pinot.core.operator.combine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import org.apache.pinot.common.utils.DataSchema;
import org.apache.pinot.core.common.DataSourceMetadata;
import org.apache.pinot.core.common.Operator;
import org.apache.pinot.core.operator.blocks.IntermediateResultsBlock;
import org.apache.pinot.core.operator.query.SelectionOrderByOperator;
import org.apache.pinot.core.query.request.context.ExpressionContext;
import org.apache.pinot.core.query.request.context.OrderByExpressionContext;
import org.apache.pinot.core.query.request.context.QueryContext;


/**
 * Combine operator for selection order-by queries.
 * <p>When the first order-by expression is an identifier (column), skip processing the segments if possible based on
 * the column min/max value and keep enough documents to fulfill the LIMIT and OFFSET requirement.
 * <ul>
 *   <li>1. Sort all the segments by the column min/max value</li>
 *   <li>2. Keep processing segments until we get enough documents to fulfill the LIMIT and OFFSET requirement</li>
 *   <li>3. Skip processing the segments that cannot add values to the final result</li>
 * </ul>
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class SelectionOrderByCombineOperator extends BaseCombineOperator {
  private static final String OPERATOR_NAME = "SelectionOrderByCombineOperator";

  // For min/max value based combine, when a thread detects that no more segments need to be processed, it inserts this
  // special IntermediateResultsBlock into the BlockingQueue to awake the main thread
  private static final IntermediateResultsBlock LAST_RESULTS_BLOCK =
      new IntermediateResultsBlock(new DataSchema(new String[0], new DataSchema.ColumnDataType[0]),
          Collections.emptyList());

  private final int _numRowsToKeep;

  List<Operator> operators;
  QueryContext queryContext;
  ExecutorService executorService;
  long endTimeMs;

  public SelectionOrderByCombineOperator(List<Operator> operators, QueryContext queryContext,
      ExecutorService executorService, long endTimeMs) {
    super(operators, queryContext, executorService, endTimeMs);
    _numRowsToKeep = queryContext.getLimit() + queryContext.getOffset();
  }

  @Override
  public String getOperatorName() {
    return OPERATOR_NAME;
  }

  @Override
  protected IntermediateResultsBlock getNextBlock() {
    List<OrderByExpressionContext> orderByExpressions = _queryContext.getOrderByExpressions();
    assert orderByExpressions != null;
    if (orderByExpressions.get(0).getExpression().getType() == ExpressionContext.Type.IDENTIFIER) {

      OrderByExpressionContext firstOrderByExpression = orderByExpressions.get(0);
      assert firstOrderByExpression.getExpression().getType() == ExpressionContext.Type.IDENTIFIER;
      String firstOrderByColumn = firstOrderByExpression.getExpression().getIdentifier();
      boolean asc = firstOrderByExpression.isAsc();
      int numOperators = _operators.size();
      List<MinMaxValueContext> minMaxValueContexts = new ArrayList<>(numOperators);
      for (Operator operator : _operators) {
        minMaxValueContexts.add(new MinMaxValueContext((SelectionOrderByOperator) operator, firstOrderByColumn));
      }
      try {
        if (asc) {
          // For ascending order, sort on column min value in ascending order
          minMaxValueContexts.sort((o1, o2) -> {
            // Put segments without column min value in the front because we always need to process them
            if (o1._minValue == null) {
              return o2._minValue == null ? 0 : -1;
            }
            if (o2._minValue == null) {
              return 1;
            }
            return o1._minValue.compareTo(o2._minValue);
          });
        } else {
          // For descending order, sort on column max value in descending order
          minMaxValueContexts.sort((o1, o2) -> {
            // Put segments without column max value in the front because we always need to process them
            if (o1._maxValue == null) {
              return o2._maxValue == null ? 0 : -1;
            }
            if (o2._maxValue == null) {
              return 1;
            }
            return o2._maxValue.compareTo(o1._maxValue);
          });
        }
      } catch (Exception e) {
        // Fall back to the default combine (process all segments) when segments have different data types for the first
        // order-by column
        LOGGER.warn("Segments have different data types for the first order-by column: {}, using the default combine",
            firstOrderByColumn);
        return super.getNextBlock();
      }

      MinMaxSelectionOrderByCombineOperator optimizedOperator =  new MinMaxSelectionOrderByCombineOperator(operators,
          queryContext, executorService, endTimeMs);
      return optimizedOperator.getNextBlock();
    } else {
      return super.getNextBlock();
    }
  }

  @Override
  protected void mergeResultsBlocks(IntermediateResultsBlock mergedBlock, IntermediateResultsBlock blockToMerge) {

  }

}