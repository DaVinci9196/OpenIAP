package org.mg.iap.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import org.mg.iap.LogUtils
import org.mg.iap.R
import org.mg.iap.core.ui.ActionType
import org.mg.iap.core.ui.AnimationType
import org.mg.iap.core.ui.BButtonGroupView
import org.mg.iap.core.ui.BClickableTextView
import org.mg.iap.core.ui.BComponent
import org.mg.iap.core.ui.BIconTextCombinationView
import org.mg.iap.core.ui.BImageView
import org.mg.iap.core.ui.BModuloImageView
import org.mg.iap.core.ui.BPlayTextView
import org.mg.iap.core.ui.BViewGroup
import org.mg.iap.core.ui.TextSpanType
import org.mg.iap.core.ui.UIType
import org.mg.iap.core.ui.ViewType
import org.mg.iap.ui.logic.SheetUIViewModel
import org.mg.iap.ui.logic.SheetUIViewState
import org.mg.iap.ui.theme.OpenIAPTheme
import org.mg.iap.ui.widgets.LoadingDialog
import org.mg.iap.ui.widgets.PasswdInputDialog

@Composable
fun SheetUIPage(viewModel: SheetUIViewModel) {
    OpenIAPTheme {
        SheetUIView(viewModel.sheetUIViewState)
        LoadingDialog(viewModel.loadingDialogVisible)
        PasswdInputDialog(viewModel.passwdInputViewState)
    }
}

@Composable
private fun SheetUIView(viewState: SheetUIViewState) {
    if (!viewState.visible)
        return
    Surface(
        modifier = Modifier
            .wrapContentHeight()
            .wrapContentWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
        ) {
            UIComponents(viewState)
        }
    }
}

@Composable
private fun UIComponents(viewState: SheetUIViewState) {
    val uiComponents = viewState.showScreen.uiComponents ?: return
    val headerComponents = uiComponents.headerComponents
    val contentComponents = uiComponents.contentComponents
    val footerComponents = uiComponents.footerComponents
    val action = viewState.showScreen.action
    if (headerComponents.isNotEmpty()) {
        Column(
            modifier = Modifier
                .wrapContentHeight(Alignment.CenterVertically)
                .wrapContentWidth()
        ) {
            for (component in headerComponents) {
                UIComponent(Modifier, component, viewState)
            }
        }
    }
    if (contentComponents.isNotEmpty()) {
        Column(
            modifier = Modifier
                .applyUITypePadding(viewState.showScreen.uiInfo?.uiType)
                .wrapContentHeight(Alignment.CenterVertically)
                .wrapContentWidth()
                .verticalScroll(rememberScrollState())
        ) {
            for (component in contentComponents) {
                UIComponent(
                    Modifier,
                    component,
                    viewState
                )
            }
        }
    }
    if (viewState.showScreen.uiInfo?.uiType == UIType.PURCHASE_ERROR_SCREEN) {
        Box(
            modifier = Modifier
                .padding(top = 40.dp)
        )
    }
    if (footerComponents.isNotEmpty()) {
        Column(
            modifier = Modifier
                .wrapContentHeight(Alignment.CenterVertically)
                .wrapContentWidth()
        ) {
            for (component in footerComponents) {
                UIComponent(Modifier, component, viewState)
            }
        }
    }
    Box(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(top = 15.dp)
    )
    if (action?.type == ActionType.DELAY && action.delay != null) {
        viewState.onClickAction(action)
    }
}

@Composable
private fun UIComponent(
    modifier: Modifier = Modifier,
    component: BComponent,
    viewState: SheetUIViewState
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .applyViewInfo(
                component.viewInfo
            )
            .wrapContentHeight(align = Alignment.CenterVertically),
    ) {
        when (val type = component.viewType) {
            ViewType.ICONTEXTCOMBINATIONVIEW -> IconTextCombinationView(
                modifier = applyAlignment(Modifier, component.viewInfo),
                component.iconTextCombinationView!!
            )

            ViewType.CLICKABLETEXTVIEW -> ClickableTextView(
                modifier = applyAlignment(Modifier, component.viewInfo),
                component.clickableTextView!!
            )

            ViewType.VIEWGROUP -> ViewGroup(
                modifier = applyAlignment(Modifier, component.viewInfo),
                component.viewGroup!!
            )

            ViewType.DIVIDERVIEW -> DividerView(
                modifier = applyAlignment(Modifier, component.viewInfo)
            )

            ViewType.MODULOIMAGEVIEW -> ModuloImageView(
                modifier = applyAlignment(Modifier, component.viewInfo),
                component.moduloImageView!!
            )

            ViewType.BUTTONGROUPVIEW -> ButtonGroupView(
                modifier = applyAlignment(Modifier, component.viewInfo),
                viewState,
                component.buttonGroupView!!
            )

            else -> LogUtils.d("invalid component type $type")
        }
    }
}

@Composable
private fun PlayTextView(
    modifier: Modifier = Modifier,
    data: BPlayTextView
) {
    val textColor = getColorByType(data.textInfo?.colorType)
        ?: LocalContentColor.current
    var textValue = ""
    if (data.textSpan.isNotEmpty()) {
        data.textSpan.forEach {
            when (it.textSpanType) {
                TextSpanType.BULLETSPAN -> textValue += "\u2022 "
                else -> {
                    LogUtils.d("Unknown TextSpan type")
                }
            }
        }
    }
    textValue += data.text
    HtmlText(
        text = textValue,
        color = textColor,
        fontSize = getFontSizeByType(data.textInfo?.styleType),
        textAlign = getTextAlignment(data.textInfo),
        maxLines = data.textInfo?.maxLines ?: Int.MAX_VALUE,
        modifier = modifier.applyViewInfo(data.viewInfo)
    )
}

@Composable
private fun IconTextCombinationView(
    modifier: Modifier = Modifier,
    data: BIconTextCombinationView
) {
    Column(
        modifier = modifier
            .wrapContentHeight()
            .wrapContentWidth()
    ) {
        //角标
        data.badgeTextView?.let {
            PlayTextView(
                modifier = applyAlignment(Modifier, it.viewInfo),
                data = it
            )
        }
        Row(
            modifier = Modifier
                .wrapContentHeight()
                .wrapContentWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            data.imageView?.let {
                ImageView(
                    data = it,
                    modifier = applyAlignment(Modifier, it.viewInfo),
                )
            }
            data.playTextView?.let {
                PlayTextView(modifier = applyAlignment(Modifier, it.viewInfo), data = it)
            }
            if (data.singleLineTextViewList == null)
                return
            Column(
                modifier = applyAlignment(Modifier, data.viewInfo)
                    .applyViewInfo(data.viewInfo)
                    .wrapContentHeight()
                    .wrapContentWidth()
            ) {
                for (singleLineTextView in data.singleLineTextViewList!!) {
                    Row(
                        modifier = Modifier
                            .wrapContentWidth()
                            .wrapContentHeight()
                    ) {
                        singleLineTextView.playTextView1?.let {
                            PlayTextView(
                                applyAlignment(
                                    Modifier,
                                    it.viewInfo
                                ).wrapContentWidth(align = Alignment.CenterHorizontally),
                                data = it
                            )
                        }
                        singleLineTextView.playTextView2?.let {
                            PlayTextView(
                                applyAlignment(
                                    Modifier,
                                    it.viewInfo
                                ).fillMaxWidth(),
                                data = it
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClickableTextView(
    modifier: Modifier = Modifier,
    data: BClickableTextView
) {
    val playTextView = data.playTextView ?: return
    if (playTextView.text.isEmpty())
        return
    PlayTextView(modifier, data = playTextView)
}

@Composable
private fun ButtonGroupView(
    modifier: Modifier = Modifier,
    viewState: SheetUIViewState,
    data: BButtonGroupView
) {
    if (data.buttonViewList.isNullOrEmpty())
        return
    Row(
        modifier = modifier
            .wrapContentHeight(align = Alignment.CenterVertically)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        data.buttonViewList.forEach {
            Button(
                onClick = { viewState.onClickAction(it.action) },
                shape = RoundedCornerShape(10),
                modifier = modifier
                    .weight(1f)
                    .defaultMinSize(minWidth = 1.dp, minHeight = 1.dp),
                contentPadding = PaddingValues(7.dp)
            ) {
                Text(
                    text = it.text
                )
            }
        }
    }
}

@Composable
private fun ImageView(
    modifier: Modifier = Modifier,
    data: BImageView
) {
    val imageLoader = ImageLoader.Builder(LocalContext.current).components {
        add(SvgDecoder.Factory())
    }.build()
    when (data.viewInfo?.contentDescription) {
        "Google Play" -> {
            Icon(
                painter = rememberAsyncImagePainter(R.raw.google_play_white, imageLoader),
                modifier = modifier.applyViewInfo(data.viewInfo),
                contentDescription = data.viewInfo?.contentDescription
            )
        }

        else -> {
            ((if (isSystemInDarkTheme()) data.darkUrl else data.lightUrl) ?: data.darkUrl
            ?: data.lightUrl)?.let {
                AsyncImage(
                    modifier = modifier.applyViewInfo(data.viewInfo),
                    model = it,
                    contentDescription = data.viewInfo?.contentDescription
                )
            }
            data.animation?.let {
                if (it.type == AnimationType.CHECK_MARK.value) {
                    AnimatedVector(
                        modifier = modifier.applyViewInfo(data.viewInfo),
                        R.drawable.anim_check_mark
                    )
                }
            }
            data.iconView?.let {
                if (it.type == 27) {
                    Icon(
                        painter = rememberAsyncImagePainter(R.raw.icon, imageLoader),
                        modifier = modifier
                            .applyViewInfo(data.viewInfo)
                            .size(11.dp),
                        contentDescription = data.viewInfo?.contentDescription
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuloImageView(
    modifier: Modifier = Modifier,
    data: BModuloImageView
) {
    data.imageView?.let {
        ImageView(modifier = modifier, data = it)
    }
}

@Composable
private fun ViewGroup(
    modifier: Modifier = Modifier,
    data: BViewGroup
) {
    Row(
        modifier = modifier
            .wrapContentHeight()
            .wrapContentHeight()
    ) {
        data.imageView2?.let {
            ImageView(modifier = applyAlignment(Modifier, it.viewInfo), data = it)
        }
        data.playTextView?.let {
            PlayTextView(modifier = applyAlignment(Modifier, it.viewInfo), data = it)
        }
        data.imageView3?.let {
            ImageView(modifier = applyAlignment(Modifier, it.viewInfo), data = it)
        }
    }
}

@Composable
private fun DividerView(
    modifier: Modifier = Modifier
) {
    Divider(
        modifier = modifier
    )
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
private fun AnimatedVector(modifier: Modifier = Modifier, @DrawableRes drawableId: Int) {
    val image = AnimatedImageVector.animatedVectorResource(drawableId)
    var atEnd by remember { mutableStateOf(false) }
    Image(
        painter = rememberAnimatedVectorPainter(image, atEnd),
        contentDescription = "",
        modifier = modifier
    )
    atEnd = true
}