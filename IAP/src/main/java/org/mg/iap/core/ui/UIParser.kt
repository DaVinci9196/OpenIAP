package org.mg.iap.core.ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.mg.iap.core.AcquireParams
import org.mg.iap.core.PurchaseItem
import org.mg.iap.core.parsePurchaseItem
import org.mg.iap.proto.AcquireResponse
import org.mg.iap.proto.Action
import org.mg.iap.proto.Animation
import org.mg.iap.proto.ButtonGroupView
import org.mg.iap.proto.ClickableTextView
import org.mg.iap.proto.ContentComponent
import org.mg.iap.proto.DividerView
import org.mg.iap.proto.FooterComponent
import org.mg.iap.proto.IconTextCombinationView
import org.mg.iap.proto.ImageView
import org.mg.iap.proto.ModuloImageView
import org.mg.iap.proto.PlayTextView
import org.mg.iap.proto.Screen
import org.mg.iap.proto.SingleLineTextView
import org.mg.iap.proto.TextInfo
import org.mg.iap.proto.ViewGroup
import org.mg.iap.proto.ViewInfo
import org.mg.iap.proto.actionOrNull
import org.mg.iap.core.responseBundleToMap
import org.mg.iap.proto.BulletSpan
import org.mg.iap.proto.ButtonView
import org.mg.iap.proto.IconView
import org.mg.iap.proto.ImageGroup
import org.mg.iap.proto.ImageInfo
import org.mg.iap.proto.InstrumentItemView
import org.mg.iap.proto.PurchaseResponse
import org.mg.iap.proto.TextSpan
import org.mg.iap.proto.UIInfo
import org.mg.iap.proto.UiComponents
import org.mg.iap.proto.purchaseResponseOrNull
import org.mg.iap.proto.uiComponentsOrNull
import org.mg.iap.proto.uiInfoOrNull

data class AcquireParsedResult(
    val action: BAction? = null,
    val result: Map<String, Any> = mapOf("RESPONSE_CODE" to 4, "DEBUG_MESSAGE" to ""),
    val purchaseItems: List<PurchaseItem>,
    val screenMap: Map<String, BScreen> = emptyMap()
)

private fun typeToDpSize(type: Int): Float {
    return when (type) {
        1 -> 2.0f
        2 -> 4.0f
        3 -> 8.0f
        4 -> 12.0f
        5 -> 16.0f
        6 -> 24.0f
        7 -> 32.0f
        8 -> 24.0f
        9 -> 18.0f
        10 -> 0.0f
        11 -> 6.0f
        else -> {
            0.0f
        }
    }
}

private fun parseUIInfo(uiInfo: UIInfo): BUIInfo {
    if (uiInfo.classType == 1) return BUIInfo(UIType.UNKNOWN)
    return BUIInfo(UIType.fromValue(uiInfo.uiType))
}

private fun parseScreen(screen: Screen): BScreen {
    var uiInfo: BUIInfo? = null
    var uiComponents: BUIComponents? = null
    var action: BAction? = null
    screen.uiInfoOrNull?.let {
        uiInfo = parseUIInfo(it)
    }
    screen.uiComponentsOrNull?.let {
        uiComponents = parseScreenComponents(it)
    }
    screen.actionOrNull?.let {
        action = BAction(ActionType.UNKNOWN)
        parseAction(it, action!!)
    }
    return BScreen(uiInfo, action, uiComponents)
}

private fun parseScreenMap(screenMap: Map<String, Screen>): Map<String, BScreen> {
    val result: MutableMap<String, BScreen> = mutableMapOf()
    for ((screenId, screen) in screenMap) {
        val bScreen = parseScreen(screen)
        result[screenId] = bScreen
    }
    return result
}

fun parseAcquireResponse(acquireResponse: AcquireResponse): BAcquireResult {
    val action = BAction(ActionType.UNKNOWN)
    parseAction(acquireResponse.action, action)
    val screenMap = parseScreenMap(acquireResponse.screenMap)
    return BAcquireResult(action, screenMap)
}

private fun parseAction(action: Action?, result: BAction): Boolean {
    if (action?.actionContext?.isEmpty == false) {
        result.actionContext.add(action.actionContext.toByteArray())
    }
    if (action?.hasTimerAction() == true) {
        result.delay = action.timerAction.delay
        result.type = ActionType.DELAY
        result.result = responseBundleToMap(action.timerAction.responseBundle)
        return true
    }
    if (action?.hasActionExt() == true && action.actionExt.hasExtAction()) {
        val extAction = action.actionExt.extAction
        if (extAction.hasDroidGuardMap()) {
            result.droidGuardMap = extAction.droidGuardMap.mapMap
        }
        if (extAction.hasAction()) {
            return parseAction(extAction.action, result)
        }
    }
    if (action?.hasShowAction() == true) {
        result.type = ActionType.SHOW
        result.screenId = action.showAction.screenId
        if (action.showAction.hasAction1()) {
            parseAction(action.showAction.action1, result)
        }
        if (action.showAction.hasAction()) {
            parseAction(action.showAction.action1, result)
        }
        return true
    }
    if (action?.hasViewClickAction() == true) {
        if (action.viewClickAction.hasUiInfo() && result.uiInfo == null) {
            result.uiInfo = parseUIInfo(action.viewClickAction.uiInfo)
        }
        return parseAction(action.viewClickAction.action, result)
    }
    if (action?.hasOptionalAction() == true) {
        return parseAction(action.optionalAction.action1, result)
    }
    if (action?.hasNavigateToPage() == true) {
        result.srcScreenId = action.navigateToPage.from
        return parseAction(action.navigateToPage.action, result)
    }
    return false
}

private fun parseAnimation(animation: Animation): BAnimation {
    var type: Int? = null
    var repeatCount: Int? = null
    if (animation.type != 0) {
        type = animation.type
    }
    if (animation.repeatCount != 0) {
        repeatCount = animation.repeatCount
    }
    return BAnimation(type, repeatCount)
}

private fun parseIconView(iconView: IconView): BIconView {
    var type: Int? = null
    var text: String? = null
    if (iconView.type != 0) {
        type = iconView.type
    }
    if (iconView.text.isNotBlank()) {
        text = iconView.text
    }
    return BIconView(type, text)
}

private fun parseInstrumentItemView(instrumentItemView: InstrumentItemView): BInstrumentItemView {
    var icon: BImageView? = null
    var text: BPlayTextView? = null
    var tips: BPlayTextView? = null
    var extraInfo: BPlayTextView? = null
    var state: BImageView? = null
    var action: BAction? = null
    if (instrumentItemView.hasIcon()) {
        icon = parseImageView(instrumentItemView.icon)
    }
    if (instrumentItemView.hasText()) {
        text = parsePlayTextView(instrumentItemView.text)
    }
    if (instrumentItemView.hasTips()) {
        tips = parsePlayTextView(instrumentItemView.tips)
    }
    if (instrumentItemView.hasState()) {
        state = parseImageView(instrumentItemView.state)
    }
    if (instrumentItemView.hasAction()) {
        action = BAction(ActionType.UNKNOWN)
        parseAction(instrumentItemView.action, action)
    }
    if (instrumentItemView.hasExtraInfo()) {
        extraInfo = parsePlayTextView(instrumentItemView.extraInfo)
    }
    return BInstrumentItemView(icon, text, tips, extraInfo, state, action)
}

private fun parseImageGroup(imageGroup: ImageGroup): BImageGroup {
    val imageViews = mutableListOf<BImageView>()
    var viewInfo: BViewInfo? = null

    if (imageGroup.hasViewInfo()) {
        viewInfo = parseViewInfo(imageGroup.viewInfo)
    }
    imageGroup.imageViewList.forEach {
        imageViews.add(parseImageView(it))
    }
    return BImageGroup(imageViews, viewInfo)
}

private fun parseImageView(imageView: ImageView): BImageView {
    var darkUrl: String? = null
    var lightUrl: String? = null
    var viewInfo: BViewInfo? = null
    var imageInfo: BImageInfo? = null
    var iconView: BIconView? = null
    var animation: BAnimation? = null
    if (imageView.hasThumbnailImageView()) {
        if (imageView.thumbnailImageView.darkUrl.isNotBlank())
            darkUrl = imageView.thumbnailImageView.darkUrl
        if (imageView.thumbnailImageView.lightUrl.isNotBlank())
            lightUrl = imageView.thumbnailImageView.lightUrl
    }
    if (imageView.hasViewInfo()) {
        viewInfo = parseViewInfo(imageView.viewInfo)
    }
    if (imageView.hasImageInfo()) {
        imageInfo = parseImageInfo(imageView.imageInfo)
    }
    if (imageView.hasIconView()) {
        iconView = parseIconView(imageView.iconView)
    }
    if (imageView.hasAnimation()) {
        animation = parseAnimation(imageView.animation)
    }
    return BImageView(viewInfo, imageInfo, lightUrl, darkUrl, animation, iconView)
}

private fun parseTextInfo(textInfo: TextInfo): BTextInfo {
    var colorType: ColorType? = null
    var maxLines: Int? = null
    var gravityList: List<BGravity>? = null
    var textAlignmentType: TextAlignmentType? = null
    var styleType: Int? = null
    if (textInfo.maxLines != 0) {
        maxLines = textInfo.maxLines
    }
    if (textInfo.gravityCount > 0) {
        gravityList = textInfo.gravityList.map { BGravity.values()[it] }
    }
    if (textInfo.hasTextColorType()) {
        colorType = ColorType.values()[textInfo.textColorType]
    }
    if (textInfo.textAlignmentType != 0) {
        textAlignmentType = TextAlignmentType.values()[textInfo.textAlignmentType]
    }
    if (textInfo.styleType != 0) {
        styleType = textInfo.styleType
    }
    return BTextInfo(colorType, maxLines, gravityList, textAlignmentType, styleType)
}

private fun parseBulletSpan(bulletSpan: BulletSpan): BBulletSpan {
    return BBulletSpan(bulletSpan.gapWidth.unitValue.toInt())
}

private fun parseTextSpan(textSpan: TextSpan): BTextSpan {
    return when (textSpan.spanCase) {
        TextSpan.SpanCase.BULLETSPAN -> BTextSpan(
            TextSpanType.BULLETSPAN,
            parseBulletSpan(textSpan.bulletSpan)
        )

        else -> BTextSpan(TextSpanType.UNKNOWNSPAN)
    }
}

private fun parsePlayTextView(playTextView: PlayTextView): BPlayTextView {
    var textInfo: BTextInfo? = null
    var viewInfo: BViewInfo? = null
    var textSpanList: MutableList<BTextSpan> = mutableListOf()
    if (playTextView.hasTextInfo()) {
        textInfo = parseTextInfo(playTextView.textInfo)
    }
    if (playTextView.hasViewInfo()) {
        viewInfo = parseViewInfo(playTextView.viewInfo)
    }
    if (playTextView.textSpanCount > 0) {
        playTextView.textSpanList.forEach {
            textSpanList.add(parseTextSpan(it))
        }
    }
    return BPlayTextView(playTextView.text, playTextView.isHtml, textInfo, viewInfo, textSpanList)
}

private fun parseSingleLineTextView(singleLineTextView: SingleLineTextView): BSingleLineTextView {
    var playTextView1: BPlayTextView? = null
    var playTextView2: BPlayTextView? = null

    if (singleLineTextView.hasPlayTextView1())
        playTextView1 = parsePlayTextView(singleLineTextView.playTextView1)
    if (singleLineTextView.hasPlayTextView2())
        playTextView2 = parsePlayTextView(singleLineTextView.playTextView2)

    return BSingleLineTextView(playTextView1, playTextView2)
}

private fun parseIconTextCombinationView(iconTextCombinationView: IconTextCombinationView): BIconTextCombinationView {
    var headerImageView: BImageView? = null
    var playTextView: BPlayTextView? = null
    var badgeTextView: BPlayTextView? = null
    var middleTextViewList: List<BSingleLineTextView>? = null
    var viewInfo: BViewInfo? = null
    var footerImageGroup: BImageGroup? = null

    if (iconTextCombinationView.hasHeaderImageView()) {
        headerImageView = parseImageView(iconTextCombinationView.headerImageView)
    }
    if (iconTextCombinationView.hasPlayTextView()) {
        playTextView = parsePlayTextView(iconTextCombinationView.playTextView)
    }
    if (iconTextCombinationView.hasBadgeTextView()) {
        badgeTextView = parsePlayTextView(iconTextCombinationView.badgeTextView)
    }
    if (iconTextCombinationView.singleLineTextViewCount > 0) {
        middleTextViewList =
            iconTextCombinationView.singleLineTextViewList.map { parseSingleLineTextView(it) }
    }
    if (iconTextCombinationView.hasFooterImageGroup()) {
        footerImageGroup = parseImageGroup(iconTextCombinationView.footerImageGroup)
    }
    if (iconTextCombinationView.hasViewInfo()) {
        viewInfo = parseViewInfo(iconTextCombinationView.viewInfo)
    }

    return BIconTextCombinationView(
        headerImageView,
        playTextView,
        badgeTextView,
        middleTextViewList,
        footerImageGroup,
        viewInfo
    )
}

private fun parseClickableTextView(clickableTextView: ClickableTextView): BClickableTextView {
    var playTextView: BPlayTextView? = null
    if (clickableTextView.hasPlayTextView())
        playTextView = parsePlayTextView(clickableTextView.playTextView)
    return BClickableTextView(playTextView)
}

private fun parseViewGroup(viewGroup: ViewGroup): BViewGroup {
    var imageView1: BImageView? = null
    var imageView2: BImageView? = null
    var imageView3: BImageView? = null
    var imageView4: BImageView? = null
    var playTextView: BPlayTextView? = null
    if (viewGroup.hasImageView1()) {
        imageView1 = parseImageView(viewGroup.imageView1)
    }
    if (viewGroup.hasImageView2()) {
        imageView2 = parseImageView(viewGroup.imageView2)
    }
    if (viewGroup.hasImageView3()) {
        imageView3 = parseImageView(viewGroup.imageView3)
    }
    if (viewGroup.hasImageView4()) {
        imageView4 = parseImageView(viewGroup.imageView4)
    }
    if (viewGroup.hasPlayTextView()) {
        playTextView = parsePlayTextView(viewGroup.playTextView)
    }
    return BViewGroup(imageView1, imageView2, imageView3, imageView4, playTextView)
}

private fun parseModuloImageView(moduloImageView: ModuloImageView): BModuloImageView {
    var imageView: BImageView? = null
    if (moduloImageView.hasImageView()) {
        imageView = parseImageView(moduloImageView.imageView)
    }
    return BModuloImageView(imageView)
}

private fun parseDividerView(dividerView: DividerView): BDividerView {
    return BDividerView()
}

private fun parseImageInfo(imageInfo: ImageInfo): BImageInfo {
    var colorFilterValue: Int? = null
    var colorFilterType: Int? = null
    var filterMode: Int? = null
    var scaleType: Int? = null
    if (imageInfo.hasValue()) {
        colorFilterValue = imageInfo.value
    } else if (imageInfo.hasValueType()) {
        colorFilterType = imageInfo.valueType
    }
    if (imageInfo.modeType != 0) {
        filterMode = imageInfo.modeType
    }
    if (imageInfo.scaleType != 0) {
        scaleType = imageInfo.scaleType
    }
    return BImageInfo(colorFilterValue, colorFilterType, filterMode, scaleType)
}

private fun parseViewInfo(viewInfo: ViewInfo): BViewInfo {
    var tag: String? = null
    var width: Float? = null
    var height: Float? = null
    var startMargin: Float? = null
    var topMargin: Float? = null
    var endMargin: Float? = null
    var bottomMargin: Float? = null
    var startPadding: Float? = null
    var topPadding: Float? = null
    var endPadding: Float? = null
    var bottomPadding: Float? = null
    var contentDescription: String? = null
    var gravityList: List<BGravity>? = null
    var backgroundColorType: ColorType? = null
    var borderColorType: ColorType? = null
    var action: BAction? = null
    var visibilityType: Int? = null

    if (viewInfo.tag.isNotBlank()) {
        tag = viewInfo.tag
    }
    if (viewInfo.widthValue != 0f) {
        width = viewInfo.widthValue
    }
    if (viewInfo.heightValue != 0f) {
        height = viewInfo.heightValue
    }
    if (viewInfo.startMargin != 0f) {
        startMargin = viewInfo.startMargin
    }
    if (viewInfo.topMargin != 0f) {
        topMargin = viewInfo.topMargin
    }
    if (viewInfo.endMargin != 0f) {
        endMargin = viewInfo.endMargin
    }
    if (viewInfo.bottomMargin != 0f) {
        bottomMargin = viewInfo.bottomMargin
    }
    if (viewInfo.startPadding != 0f) {
        startPadding = viewInfo.startPadding
    }
    if (viewInfo.topPadding != 0f) {
        topPadding = viewInfo.topPadding
    }
    if (viewInfo.endPadding != 0f) {
        endPadding = viewInfo.endPadding
    }
    if (viewInfo.bottomPadding != 0f) {
        bottomPadding = viewInfo.bottomPadding
    }
    if (viewInfo.startMarginType != 0) {
        startMargin = typeToDpSize(viewInfo.startMarginType)
    }
    if (viewInfo.topMarginType != 0) {
        topMargin = typeToDpSize(viewInfo.topMarginType)
    }
    if (viewInfo.endMarginType != 0) {
        endMargin = typeToDpSize(viewInfo.endMarginType)
    }
    if (viewInfo.bottomMarginType != 0) {
        bottomMargin = typeToDpSize(viewInfo.bottomMarginType)
    }
    if (viewInfo.startPaddingType != 0) {
        startPadding = typeToDpSize(viewInfo.startPaddingType)
    }
    if (viewInfo.topPaddingType != 0) {
        topPadding = typeToDpSize(viewInfo.topPaddingType)
    }
    if (viewInfo.endPaddingType != 0) {
        endPadding = typeToDpSize(viewInfo.endPaddingType)
    }
    if (viewInfo.bottomPaddingType != 0) {
        bottomPadding = typeToDpSize(viewInfo.bottomPaddingType)
    }
    if (viewInfo.contentDescription.isNotBlank()) {
        contentDescription = viewInfo.contentDescription
    }
    if (viewInfo.gravityCount > 0) {
        gravityList = viewInfo.gravityList.map { BGravity.values()[it] }
    }
    if (viewInfo.backgroundColorType != 0) {
        backgroundColorType = ColorType.values()[viewInfo.backgroundColorType]
    }
    if (viewInfo.borderColorType != 0) {
        borderColorType = ColorType.values()[viewInfo.borderColorType]
    }
    if (viewInfo.hasAction()) {
        action = BAction(ActionType.UNKNOWN)
        parseAction(viewInfo.action, action)
    }
    if (viewInfo.visibilityType != 0) {
        visibilityType = viewInfo.visibilityType
    }
    return BViewInfo(
        tag,
        width,
        height,
        startMargin,
        topMargin,
        endMargin,
        bottomMargin,
        startPadding,
        topPadding,
        endPadding,
        bottomPadding,
        contentDescription,
        gravityList,
        backgroundColorType,
        borderColorType,
        action,
        visibilityType
    )
}

private fun parseContentComponent(contentComponent: ContentComponent): BComponent {
    val tag = contentComponent.tag
    var viewInfo: BViewInfo? = null
    var uiInfo: BUIInfo? = null
    if (contentComponent.hasViewInfo()) {
        viewInfo = parseViewInfo(contentComponent.viewInfo)
    }
    if (contentComponent.hasUiInfo()) {
        uiInfo = parseUIInfo(contentComponent.uiInfo)
    }
    return when (contentComponent.uiComponentCase) {
        ContentComponent.UiComponentCase.ICONTEXTCOMBINATIONVIEW -> {
            BComponent(
                tag,
                uiInfo,
                viewInfo,
                ViewType.ICONTEXTCOMBINATIONVIEW,
                iconTextCombinationView = parseIconTextCombinationView(contentComponent.iconTextCombinationView)
            )
        }

        ContentComponent.UiComponentCase.CLICKABLETEXTVIEW -> {
            BComponent(
                tag,
                uiInfo,
                viewInfo,
                ViewType.CLICKABLETEXTVIEW,
                clickableTextView = parseClickableTextView(contentComponent.clickableTextView)
            )
        }

        ContentComponent.UiComponentCase.VIEWGROUP -> {
            BComponent(
                tag,
                uiInfo,
                viewInfo,
                ViewType.VIEWGROUP,
                viewGroup = parseViewGroup(contentComponent.viewGroup)
            )
        }

        ContentComponent.UiComponentCase.DIVIDERVIEW -> {
            BComponent(
                tag,
                uiInfo,
                viewInfo,
                ViewType.DIVIDERVIEW,
                dividerView = parseDividerView(contentComponent.dividerView)
            )
        }

        ContentComponent.UiComponentCase.MODULOIMAGEVIEW -> {
            BComponent(
                tag,
                uiInfo,
                viewInfo,
                ViewType.MODULOIMAGEVIEW,
                moduloImageView = parseModuloImageView(contentComponent.moduloImageView)
            )
        }

        ContentComponent.UiComponentCase.BUTTONGROUPVIEW -> {
            BComponent(
                tag,
                uiInfo,
                viewInfo,
                ViewType.BUTTONGROUPVIEW,
                buttonGroupView = parseButtonGroupView(contentComponent.buttonGroupView)
            )
        }

        ContentComponent.UiComponentCase.INSTRUMENTITEMVIEW -> {
            BComponent(
                tag,
                uiInfo,
                viewInfo,
                ViewType.INSTRUMENTITEMVIEW,
                instrumentItemView = parseInstrumentItemView(contentComponent.instrumentItemView)
            )
        }

        else -> BComponent(viewType = ViewType.UNKNOWNVIEW)
    }
}

private fun parseButtonView(buttonView: ButtonView): BButtonView {
    var text = ""
    var viewInfo: BViewInfo? = null
    var action = BAction(ActionType.UNKNOWN)
    text = buttonView.text
    if (buttonView.hasViewInfo()) {
        viewInfo = parseViewInfo(buttonView.viewInfo)
    }
    if (buttonView.hasAction()) {
        parseAction(buttonView.action, action)
    }
    return BButtonView(text, viewInfo, action)
}

private fun parseButtonGroupView(buttonGroupView: ButtonGroupView): BButtonGroupView {
    var buttonViewList = mutableListOf<BButtonView>()

    if (buttonGroupView.hasNewButtonView()) {
        if (buttonGroupView.newButtonView.hasButtonView()) {
            buttonViewList.add(parseButtonView(buttonGroupView.newButtonView.buttonView))
        }
        if (buttonGroupView.newButtonView.hasButtonView2()) {
            buttonViewList.add(parseButtonView(buttonGroupView.newButtonView.buttonView2))
        }
    }
    return BButtonGroupView(buttonViewList)
}

private fun parseFooterComponent(footerComponent: FooterComponent): BComponent {
    val tag = footerComponent.tag
    var viewInfo: BViewInfo? = null
    var uiInfo: BUIInfo? = null
    if (footerComponent.hasViewInfo()) {
        viewInfo = parseViewInfo(footerComponent.viewInfo)
    }
    if (footerComponent.hasUiInfo()) {
        uiInfo = parseUIInfo(footerComponent.uiInfo)
    }
    return when (footerComponent.uiComponentCase) {
        FooterComponent.UiComponentCase.BUTTONGROUPVIEW -> {
            BComponent(
                tag,
                uiInfo,
                viewInfo,
                ViewType.BUTTONGROUPVIEW,
                buttonGroupView = parseButtonGroupView(footerComponent.buttonGroupView)
            )
        }

        FooterComponent.UiComponentCase.DIVIDERVIEW -> {
            BComponent(
                tag,
                uiInfo,
                viewInfo,
                ViewType.DIVIDERVIEW,
                dividerView = parseDividerView(footerComponent.dividerView)
            )
        }

//        FooterComponent.UiComponentCase.ICONTEXTCOMBINATIONVIEW -> {
//            BComponent(
//                tag,
//                viewInfo,
//                ViewType.ICONTEXTCOMBINATIONVIEW,
//                iconTextCombinationView = parseIconTextCombinationView(footerComponent.iconTextCombinationView)
//            )
//        }

        else -> BComponent(viewType = ViewType.UNKNOWNVIEW)
    }
}

private fun parseScreenComponents(uiComponents: UiComponents): BUIComponents {
    val headerComponents = mutableListOf<BComponent>()
    val contentComponents = mutableListOf<BComponent>()
    val footerComponents = mutableListOf<BComponent>()
    for (contentComponent in uiComponents.contentComponent1List) {
        headerComponents.add(parseContentComponent(contentComponent))
    }
    for (contentComponent in uiComponents.contentComponent2List) {
        contentComponents.add(parseContentComponent(contentComponent))
    }
    for (footerComponent in uiComponents.footerComponentList) {
        footerComponents.add(parseFooterComponent(footerComponent))
    }

    return BUIComponents(headerComponents, contentComponents, footerComponents)
}

fun parsePurchaseResponse(
    acquireParams: AcquireParams,
    purchaseResponse: PurchaseResponse?
): Pair<Map<String, Any>, PurchaseItem?> {
    if (purchaseResponse == null) {
        return mapOf<String, Any>("RESPONSE_CODE" to 0, "DEBUG_MESSAGE" to "") to null
    }
    val resultMap = responseBundleToMap(purchaseResponse.responseBundle)
    val code = resultMap["RESPONSE_CODE"] as Int? ?: return mapOf<String, Any>(
        "RESPONSE_CODE" to 0,
        "DEBUG_MESSAGE" to ""
    ) to null
    val pd = resultMap["INAPP_PURCHASE_DATA"] as String? ?: return resultMap to null
    val ps = resultMap["INAPP_DATA_SIGNATURE"] as String? ?: return resultMap to null
    if (code != 0) return resultMap to null
    val pdj = Json.parseToJsonElement(pd).jsonObject
    val packageName =
        pdj["packageName"]?.jsonPrimitive?.content ?: return resultMap to null
    val purchaseToken =
        pdj["purchaseToken"]?.jsonPrimitive?.content ?: return resultMap to null
    val purchaseState =
        pdj["purchaseState"]?.jsonPrimitive?.int ?: return resultMap to null
    return resultMap to PurchaseItem(
        acquireParams.buyFlowParams.skuType,
        acquireParams.buyFlowParams.sku,
        packageName,
        purchaseToken,
        purchaseState,
        pd, ps
    )
}

fun parseAcquireResponse(
    acquireParams: AcquireParams,
    acquireResponse: AcquireResponse
): AcquireParsedResult {
    val action = BAction(ActionType.UNKNOWN)
    parseAction(acquireResponse.action, action)
    val screenMap = parseScreenMap(acquireResponse.screenMap)
    val (result, purchaseItem) = parsePurchaseResponse(
        acquireParams,
        acquireResponse.acquireResult.purchaseResponseOrNull
    )
    val purchaseItems = mutableListOf<PurchaseItem>()
    if (purchaseItem != null) purchaseItems.add(purchaseItem)
    if (acquireResponse.acquireResult.hasOwnedPurchase()) {
        acquireResponse.acquireResult.ownedPurchase.purchaseItemList.forEach {
            purchaseItems.addAll(parsePurchaseItem(it))
        }
    }
    return AcquireParsedResult(action, result, purchaseItems, screenMap)
}