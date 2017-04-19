
package org.molgenis.downloader.emx;

import org.molgenis.downloader.api.EMXWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.molgenis.downloader.api.EntityConsumer;
import org.molgenis.downloader.api.metadata.Attribute;
import org.molgenis.downloader.api.metadata.DataType;
import org.molgenis.downloader.api.metadata.Entity;
import org.molgenis.downloader.api.EMXDataStore;
import org.molgenis.downloader.api.metadata.MolgenisVersion;
import org.molgenis.downloader.client.MolgenisRestApiClient;

class EMXEntityConsumer implements EntityConsumer {

    private final List<Attribute> attributes;
    private final EMXDataStore sheet;
    private final EMXWriter writer;

    public EMXEntityConsumer(final EMXWriter writer, final Entity entity, final MolgenisVersion version) throws IOException {
        this.writer = writer;
        attributes = setAttributes(entity);
        final List<String> values = getAttributes().stream().map(Attribute::getName).collect(Collectors.toList());

        if (version.equalsOrSmallerThan(MolgenisRestApiClient.VERSION_3))
        {
            sheet = writer.createDataStore(entity.getFullName());
        }
        else{
            sheet = writer.createDataStore(entity.getId());
        }
        sheet.writeRow(values);
    }

    @Override
    public void accept(Map<String, String> data) {
        final List<String> values = new ArrayList<>();
        for (int index = 0; index < getAttributeNames().size(); index++) {
            final String value = data.get(getAttributeNames().get(index));
            if (value != null && !value.trim().isEmpty()) {
                values.add(value.trim());
            } else {
                values.add(null);
            }
        }
        try {
            sheet.writeRow(values);
        } catch (final IOException ex) {
            writer.addException(ex);
        }
    }
    
    private List<Attribute> getAttributes() {
        return attributes;
    }

    private List<String> getAttributeNames() {
        return attributes.stream().map(Attribute::getName).collect(Collectors.toList());
    }

    private List<Attribute> getParts(final Attribute compound) {
        List<Attribute> atts = new ArrayList<>();
        compound.getParts().forEach((Attribute att) -> {
            if (att.getDataType().equals(DataType.COMPOUND)) {
                atts.addAll(getParts(att));
            } else {
                atts.add(att);
            }
        });
        return atts;
    }
    
    private List<Attribute> setAttributes(final Entity entity) {
        List<Attribute> atts = new ArrayList<>();
        entity.getAttributes().forEach((Attribute att) -> {
            if (att.getDataType().equals(DataType.COMPOUND)) {
                atts.addAll(getParts(att));
            } else {
                atts.add(att);
            }
        });
        return atts;
    }
}
