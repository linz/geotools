<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
  xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd"
  xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
  xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

  <NamedLayer>
    <Name>parcels</Name>
    <UserStyle>
      <Title>A yellow polygon style</Title>
      <FeatureTypeStyle>
        <Rule>
          <Title>green polygon</Title>
          <MaxScaleDenominator>17000</MaxScaleDenominator>
          <PolygonSymbolizer>
            <Fill>
              <CssParameter name="fill">#AAAAAA
              </CssParameter>
              <CssParameter name="fill-opacity">0.2
              </CssParameter>
            </Fill>
            <Stroke>
              <CssParameter name="stroke">#000000</CssParameter>
              <CssParameter name="stroke-width">0.5</CssParameter>
            </Stroke>
          </PolygonSymbolizer>
        </Rule>
<Rule>

          <Title>red polygon</Title>
   <ogc:Filter>
<ogc:PropertyIsGreaterThan>
     <ogc:PropertyName>area</ogc:PropertyName>
       <ogc:Literal>100000</ogc:Literal>
     </ogc:PropertyIsGreaterThan>
   </ogc:Filter>
          <MinScaleDenominator>17000</MinScaleDenominator>

          <PolygonSymbolizer>
            <Fill>
              <CssParameter name="fill">#AAAAAA
              </CssParameter>
              <CssParameter name="fill-opacity">0.2
              </CssParameter>
            </Fill>
            <Stroke>
              <CssParameter name="stroke">#000000</CssParameter>
              <CssParameter name="stroke-width">0.5</CssParameter>
            </Stroke>
          </PolygonSymbolizer>

        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>