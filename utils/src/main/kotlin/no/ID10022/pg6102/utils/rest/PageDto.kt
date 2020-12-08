package no.id10022.pg6102.utils.rest

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import javax.validation.constraints.NotNull

@ApiModel(description = "Paginated list of resources")
data class PageDto<T>(

    @ApiModelProperty("List of data contained in the page")
    @get:NotNull
    var list: List<T> = listOf(),

    @ApiModelProperty("Link to the next page")
    var next: String? = null

)
