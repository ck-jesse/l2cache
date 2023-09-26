package com.github.jesse.l2cache.example.dto;

import lombok.Data;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author chenck
 * @date 2023/9/26 17:12
 */
@Data
public class GoodsPriceRevisionIdsPutReqDTO {

    GoodsPriceRevisionIdsReqDTO goodsPriceRevisionIdsReqDTO;
    GoodsPriceRevisionRespBO goodsPriceRevisionRespBO;
}
