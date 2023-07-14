package run.acloud.commons.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;
import run.acloud.commons.vo.ListCountVO;
import run.acloud.commons.vo.PagingVO;
import run.acloud.framework.enums.ExceptionType;
import run.acloud.framework.exception.CocktailException;

@Slf4j
@Component
public final class PagingUtils {
	// Private constructor prevents instantiation from other classes
	private PagingUtils() { }

	/**
	 * Set paging 관련 param
	 *
	 * @param orderColumn
	 * @param order
	 * @param nextPage
	 * @param itemPerPage
	 * @param listCount
	 * @return
	 * @throws Exception
	 */
	public static PagingVO setPagingParams(String orderColumn, String order, Integer nextPage, Integer itemPerPage, String maxId, ListCountVO listCount) throws Exception {
		PagingVO paging = new PagingVO();

		if (nextPage != null && itemPerPage != null) {
			if(nextPage < 1) nextPage = 1;
			Integer limitNextPage = (nextPage - 1) * itemPerPage; // nextPage 값을 LIMIT Query 형태에 맞도록 변환

			paging.setNextPage(limitNextPage);
			paging.setItemPerPage(itemPerPage);
		}

		paging.setOrderColumn(orderColumn);
		paging.setOrder(order);

		if (listCount != null) {
			paging.setListCount(listCount);
			if(StringUtils.isBlank(maxId) && listCount.getMaxId() != null) {
				paging.setMaxId(paging.getListCount().getMaxId().toString());;
			}
		}

		return paging;
	}

	public static void validatePagingParams(Integer nextPage, Integer itemPerPage) throws CocktailException {
		if (nextPage == null || (nextPage != null && !NumberUtils.isDigits(nextPage.toString()))) {
			throw new CocktailException("nextPage Parameter must be of Integer.", ExceptionType.InvalidParameter);
		}
		if (itemPerPage == null || (itemPerPage != null && !NumberUtils.isDigits(itemPerPage.toString()))) {
			throw new CocktailException("itemPerPage Parameter must be of Integer.", ExceptionType.InvalidParameter);
		}
	}

	public static void validatePagingParamsOrderColumn(String orderColumn, CharSequence... allowableValues) throws CocktailException {
		if (!StringUtils.equalsAny(orderColumn, allowableValues)) {
			throw new CocktailException("orderColumn Parameter is invalid.", ExceptionType.InvalidParameter);
		}
	}

	public static void validatePagingParamsOrder(String order, CharSequence... allowableValues) throws CocktailException {
		if (!StringUtils.equalsAny(order, allowableValues)) {
			throw new CocktailException("order Parameter is invalid.", ExceptionType.InvalidParameter);
		}
	}
}
