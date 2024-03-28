package eu.openmetrics.kgg.service;

import java.util.Iterator;
import java.util.List;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import gr.tuc.ifc.IfcModel;
import gr.tuc.ifc4.IfcBuilding;
import gr.tuc.ifc4.IfcBuildingStorey;
import gr.tuc.ifc4.IfcDoor;
import gr.tuc.ifc4.IfcIdentifier;
import gr.tuc.ifc4.IfcObjectDefinition;
import gr.tuc.ifc4.IfcOpeningElement;
import gr.tuc.ifc4.IfcProduct;
import gr.tuc.ifc4.IfcProject;
import gr.tuc.ifc4.IfcProperty;
import gr.tuc.ifc4.IfcPropertySet;
import gr.tuc.ifc4.IfcPropertySingleValue;
import gr.tuc.ifc4.IfcReal;
import gr.tuc.ifc4.IfcRelAggregates;
import gr.tuc.ifc4.IfcRelAssigns;
import gr.tuc.ifc4.IfcRelAssignsToGroup;
import gr.tuc.ifc4.IfcRelContainedInSpatialStructure;
import gr.tuc.ifc4.IfcRelDefinesByProperties;
import gr.tuc.ifc4.IfcRelFillsElement;
import gr.tuc.ifc4.IfcSite;
import gr.tuc.ifc4.IfcSpace;
import gr.tuc.ifc4.IfcSpatialElement;
import gr.tuc.ifc4.IfcText;
import gr.tuc.ifc4.IfcUnitaryControlElement;
import gr.tuc.ifc4.IfcUnitaryControlElementTypeEnum;
import gr.tuc.ifc4.IfcUnitaryEquipment;
import gr.tuc.ifc4.IfcUnitaryEquipmentTypeEnum;
import gr.tuc.ifc4.IfcValue;
import gr.tuc.ifc4.IfcWindow;
import gr.tuc.ifc4.IfcZone;

@Service
public class BIMtoRDFconverter {

	private static Logger log = LoggerFactory.getLogger(BIMtoRDFconverter.class);
	
	private Model rdfModel;
	
	public BIMtoRDFconverter() {
		log.info("Knowledge Graph Generator");
	}

	public Model convert(IfcModel ifcModel) {
		rdfModel = ModelFactory.createDefaultModel();
		rdfModel.setNsPrefix("owl", OWL.getURI());
		rdfModel.setNsPrefix("rdf", RDF.getURI());
		rdfModel.setNsPrefix("rdfs", RDFS.getURI());
		rdfModel.setNsPrefix("xsd", XSD.getURI());
		rdfModel.setNsPrefix("schema", "http://schema.org#");
		rdfModel.setNsPrefix("om", "http://openmetrics.eu/openmetrics#");
		rdfModel.setNsPrefix("brick", "https://brickschema.org/schema/Brick#");
		rdfModel.setNsPrefix("ref", "https://brickschema.org/schema/Brick/ref#");
		rdfModel.setNsPrefix("saref", "https://saref.etsi.org/core#");
		rdfModel.setNsPrefix("s4ener", "https://saref.etsi.org/saref4ener#");
		rdfModel.setNsPrefix("s4bldg", "https://saref.etsi.org/saref4bldg#");
		
		
		Iterator<IfcProject> projectIterator = ifcModel.getAllE(IfcProject.class).iterator();
		if(projectIterator.hasNext()) {
			IfcProject ifcProject = projectIterator.next();
			parseProject(ifcProject);
		}
		return rdfModel;
	}
	
	private void parseProject(IfcProject ifcProject) {
		Iterator<IfcRelAggregates> projectRelAggregatesIterator = ifcProject.getIsDecomposedBy().iterator();
		while(projectRelAggregatesIterator.hasNext()) {
			IfcRelAggregates projectRelAggregates = projectRelAggregatesIterator.next();
			List<IfcObjectDefinition> projectRelatedObjectList = projectRelAggregates.getRelatedObjects();
			for(IfcObjectDefinition projectRelatedObject : projectRelatedObjectList) {
				if(projectRelatedObject instanceof IfcSite) {
					IfcSite ifcSite = (IfcSite) projectRelatedObject;
					//
					Resource resSiteIfc = rdfModel.createResource();
					resSiteIfc.addProperty(RDF.type, ResourceFactory.createResource( rdfModel.getNsPrefixURI("ref") + "IFCReference"));
					resSiteIfc.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"), ifcSite.getName() != null ? ifcSite.getName().getValue() : "Undefined" );
					resSiteIfc.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"), ResourceFactory.createStringLiteral( ifcSite.getGlobalId().getValue() ) );
					//
					Resource resSite =  rdfModel.createResource(rdfModel.getNsPrefixURI("om") + "Site_" + ifcSite.getExpressId());
					resSite.addProperty(RDF.type, ResourceFactory.createResource( rdfModel.getNsPrefixURI("brick") + "Site"));
					resSite.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),  resSiteIfc );
					//
					Iterator<IfcRelAggregates> siteRelAggregatesIterator = ifcSite.getIsDecomposedBy().iterator();
					while(siteRelAggregatesIterator.hasNext()) {
						IfcRelAggregates siteRelAggregates = siteRelAggregatesIterator.next();
						List<IfcObjectDefinition> siteRelatedObjectList = siteRelAggregates.getRelatedObjects();
						for(IfcObjectDefinition siteRelatedObject : siteRelatedObjectList) {
							if(siteRelatedObject instanceof IfcBuilding) {
								IfcBuilding building = (IfcBuilding) siteRelatedObject;
								parseBuilding(building, resSite);
							}else {
								log.warn("unsupported case");
							}
						}
					}
				}else if (projectRelatedObject instanceof IfcBuilding) {
					IfcBuilding building = (IfcBuilding) projectRelatedObject;
					parseBuilding(building, null);					
				}else {
					log.warn("usupported case");
				}
			}
		}
	}

	private void parseBuilding(IfcBuilding ifcBuilding, Resource resSite) {
		//
		Resource resBuildingIfcRef = rdfModel.createResource();
		resBuildingIfcRef.addProperty(RDF.type, ResourceFactory.createResource( rdfModel.getNsPrefixURI("ref") + "IFCReference"));
		resBuildingIfcRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"), ifcBuilding.getName() != null ? ifcBuilding.getName().getValue() : "Undefined" );
		resBuildingIfcRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"), ResourceFactory.createStringLiteral( ifcBuilding.getGlobalId().getValue() ) );
		//
		Resource resBuilding = rdfModel.createResource(rdfModel.getNsPrefixURI("om") + "Building_" + ifcBuilding.getExpressId());
		resBuilding.addProperty(RDF.type, ResourceFactory.createResource( rdfModel.getNsPrefixURI("brick") + "Building"));
		resBuilding.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),  resBuildingIfcRef );
		//
		//
		//
		// update parent
		if(resSite != null) {
			resSite.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPart"), resBuilding);		
		}
		// using containedInSpatialStructure
		Iterator<IfcRelContainedInSpatialStructure> buildingContainedInSpatialStructureIterator = ifcBuilding.getContainsElements().iterator();
		while(buildingContainedInSpatialStructureIterator.hasNext()) {
			IfcRelContainedInSpatialStructure relContainedInSpatialStructure = buildingContainedInSpatialStructureIterator.next();
			for(IfcProduct product : relContainedInSpatialStructure.getRelatedElements()) {
				parseProduct(product, resBuilding);
			}
		}
		// using decomposedBy
		Iterator<IfcRelAggregates> buildingRelAggregatesIterator = ifcBuilding.getIsDecomposedBy().iterator();
		while(buildingRelAggregatesIterator.hasNext()) {
			IfcRelAggregates buildingRelAggregates = buildingRelAggregatesIterator.next();
			List<IfcObjectDefinition> buildingRelatedObjectList = buildingRelAggregates.getRelatedObjects();
			for(IfcObjectDefinition builidngRelatedObject : buildingRelatedObjectList) {
				if(builidngRelatedObject instanceof IfcBuildingStorey) {
					IfcBuildingStorey ifcBuildingStorey = (IfcBuildingStorey) builidngRelatedObject;
					//
					Resource resBuildingStoreyIfcRef = rdfModel.createResource( );
					resBuildingStoreyIfcRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"), ifcBuilding.getName() != null ? ifcBuilding.getName().getValue() : "Undefined" );
					resBuildingStoreyIfcRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"), ResourceFactory.createStringLiteral( ifcBuilding.getGlobalId().getValue() ) );
					//
					Resource resStorey = rdfModel.createResource(rdfModel.getNsPrefixURI("om") + "BuildingStorey_" + ifcBuildingStorey.getExpressId());
					resStorey.addProperty(RDF.type, ResourceFactory.createResource( rdfModel.getNsPrefixURI("brick") + "Storey"));
					resStorey.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),  resBuildingStoreyIfcRef );
					//
					// parent resource
					resBuilding.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPart"), resStorey);
					// using decomposedBy
					Iterator<IfcRelAggregates> buildingStoreyRelAggregatesIterator = ifcBuildingStorey.getIsDecomposedBy().iterator(); 
					while(buildingStoreyRelAggregatesIterator.hasNext()) {
						IfcRelAggregates buildingStoreyRelAggregates = buildingStoreyRelAggregatesIterator.next();
						List<IfcObjectDefinition> buildingStoreyRelatedObjectList = buildingStoreyRelAggregates.getRelatedObjects();
						for(IfcObjectDefinition buildingStoreyRelatedObject : buildingStoreyRelatedObjectList) {
							if(buildingStoreyRelatedObject instanceof IfcProduct) {
								IfcProduct product = (IfcProduct) buildingStoreyRelatedObject;					
								parseProduct(product, resStorey);
							}else {
								log.warn("unsupported case");
							}
						}
					}
					// using containedInSpatialStructure
					Iterator<IfcRelContainedInSpatialStructure> buildingStoreyContainedInSpatialStructureIterator = ifcBuildingStorey.getContainsElements().iterator();
					while(buildingStoreyContainedInSpatialStructureIterator.hasNext()) {
						IfcRelContainedInSpatialStructure relContainedInSpatialStructure = buildingStoreyContainedInSpatialStructureIterator.next();
						for(IfcProduct product : relContainedInSpatialStructure.getRelatedElements()) {
							parseProduct(product, resStorey);
						}
					}
				}else {
					log.warn("unsupported case");
				}
			}
		}
	}

	private void parseProduct(IfcProduct product, Resource resParent) {
		if(product instanceof IfcSpace) {
			IfcSpace ifcSpace = (IfcSpace) product;
			//
			Resource resSpaceIfcRef = rdfModel.createResource();
			resSpaceIfcRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"), ifcSpace.getName() != null ? ifcSpace.getName().getValue() : "Undefined" );
			resSpaceIfcRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"), ResourceFactory.createStringLiteral( ifcSpace.getGlobalId().getValue() ) );		
			//
			Resource resSpace = rdfModel.createResource(rdfModel.getNsPrefixURI("om") + "Space_" + ifcSpace.getExpressId());
			resSpace.addProperty(RDF.type, ResourceFactory.createResource( rdfModel.getNsPrefixURI("brick") + "Space"));	
			resSpace.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"), resSpaceIfcRef);
			//
			resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPart"), resSpace);
			
			//
			// properties
			parsePsets(ifcSpace, resSpace);
			//
			// zones
			Iterator<IfcRelAssigns> spaceAssignmentIterator = ifcSpace.getHasAssignments().iterator();
			while(spaceAssignmentIterator.hasNext()) {
				IfcRelAssigns relAssigns = spaceAssignmentIterator.next();
				if(relAssigns instanceof IfcRelAssignsToGroup) {
					IfcRelAssignsToGroup relAssignsToGroup = (IfcRelAssignsToGroup) relAssigns;
					if(relAssignsToGroup.getRelatingGroup() instanceof IfcZone) {
						IfcZone ifcZone = (IfcZone) relAssignsToGroup.getRelatingGroup();
						//
						//
						Resource resZoneIfcRef = rdfModel.createResource(rdfModel.getNsPrefixURI("om") + "ZoneRef_" + ifcZone.getExpressId());
						resZoneIfcRef.addProperty(RDF.type, ResourceFactory.createResource( rdfModel.getNsPrefixURI("ref") + "IFCReference"));
						resZoneIfcRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"), ifcZone.getName() != null ? ifcZone.getName().getValue() : "Undefined" );
						resZoneIfcRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"), ResourceFactory.createStringLiteral( ifcZone.getGlobalId().getValue() ) );
						//
						//
						Resource resZone = rdfModel.createResource(rdfModel.getNsPrefixURI("om") + "Zone_" + ifcZone.getExpressId());
						resZone.addProperty(RDF.type, ResourceFactory.createResource( rdfModel.getNsPrefixURI("brick") + "Zone"));
						resZone.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"), resZoneIfcRef);
						resZone.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPart"), resSpace);
						//
					}else {
						log.warn("unsupported case");
					}
				}else {
					log.warn("unsupported case");
				}
			}
			// products included in the space
			Iterator<IfcRelContainedInSpatialStructure> buildingContainedInSpatialStructureIterator = ifcSpace.getContainsElements().iterator();
			while(buildingContainedInSpatialStructureIterator.hasNext()) {
				IfcRelContainedInSpatialStructure relContainedInSpatialStructure = buildingContainedInSpatialStructureIterator.next();
				for(IfcProduct spaceProduct : relContainedInSpatialStructure.getRelatedElements()) {		
					parseProduct(spaceProduct, resSpace);
				}
			}	
		}else if(product instanceof IfcUnitaryEquipment) {
			IfcUnitaryEquipment ifcUnitaryEquipment = (IfcUnitaryEquipment) product;
			if(ifcUnitaryEquipment.getPredefinedType().equals(IfcUnitaryEquipmentTypeEnum.SPLITSYSTEM)) {
				//
				Resource resSplitSystemRef = rdfModel.createResource();
				resSplitSystemRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"), ifcUnitaryEquipment.getName() != null ? ifcUnitaryEquipment.getName().getValue() : "Undefined" );
				resSplitSystemRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"), ResourceFactory.createStringLiteral( ifcUnitaryEquipment.getGlobalId().getValue() ) );
				//				
				Resource resSplitSystem = rdfModel.createResource(rdfModel.getNsPrefixURI("om") + "Element_" + ifcUnitaryEquipment.getExpressId() );
				resSplitSystem.addProperty(RDF.type, ResourceFactory.createResource( rdfModel.getNsPrefixURI("brick") + "Terminal_Unit"));
				resSplitSystem.addProperty(RDF.type, ResourceFactory.createResource( rdfModel.getNsPrefixURI("saref") + "HVAC"));
				resSplitSystem.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"), resSplitSystemRef);
				resSplitSystem.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasLocation"), resParent);
				//
				// parent
				resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isLocationOf"), resSplitSystem);
				//
				Iterator<IfcRelContainedInSpatialStructure> containedInSpatialStructureIterator = ifcUnitaryEquipment.getContainedInStructure().iterator();
				while(containedInSpatialStructureIterator.hasNext()) {
					IfcRelContainedInSpatialStructure relContainedInSpatialStructure = containedInSpatialStructureIterator.next();		
					IfcSpatialElement spatialElement = relContainedInSpatialStructure.getRelatingStructure();	
					if(spatialElement instanceof IfcSpace) {
						IfcSpace ifcSpace = (IfcSpace) spatialElement;
						Iterator<IfcRelAssigns> relAssignIterator = ifcSpace.getHasAssignments().iterator();
						while(relAssignIterator.hasNext()) {		
							IfcRelAssigns relAssigns = relAssignIterator.next();
							if(relAssigns instanceof IfcRelAssignsToGroup) {
								IfcRelAssignsToGroup relAssignsToGroup = (IfcRelAssignsToGroup) relAssigns;
								if(relAssignsToGroup.getRelatingGroup() instanceof IfcZone) {
									IfcZone ifcZone = (IfcZone) relAssignsToGroup.getRelatingGroup();
									Resource resZone = rdfModel.createResource(rdfModel.getNsPrefixURI("om") + "Zone_" + ifcZone.getExpressId());
									resSplitSystem.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "feeds"), resZone);
								}
							}
						}
					}else {
						log.warn("unsupported case");
					}
				}
				// add psets
				parsePsets(ifcUnitaryEquipment, resSplitSystem);
									
			}else {
				log.warn("unsupported case");
			}
			
		}else if(product instanceof IfcUnitaryControlElement) {
			IfcUnitaryControlElement ifcUnitaryControlElement = (IfcUnitaryControlElement) product;		
			if(ifcUnitaryControlElement.getPredefinedType().equals(IfcUnitaryControlElementTypeEnum.THERMOSTAT)) {
				
				
				
				
				Resource resControlPanel = rdfModel.createResource( rdfModel.getNsPrefixURI("om") + getSerial(product) + "_Controller" );
				
				Resource resControlPanelRef = rdfModel.createResource();
				resControlPanelRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"), ifcUnitaryControlElement.getName() != null ? ifcUnitaryControlElement.getName().getValue() : "Undefined" );
				resControlPanelRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"), ResourceFactory.createStringLiteral( ifcUnitaryControlElement.getGlobalId().getValue() ) );
				
				resControlPanel.addProperty(RDF.type, ResourceFactory.createResource( rdfModel.getNsPrefixURI("brick") + "Thermostat"));
				resControlPanel.addProperty(RDF.type, ResourceFactory.createResource( rdfModel.getNsPrefixURI("s4bldg") + "UnitaryControlElement"));
				resControlPanel.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"), resControlPanelRef);
							
				resControlPanel.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasLocation"), resParent);
				resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isLocationOf"), resControlPanel);
				
				// add psets
				parsePsets(ifcUnitaryControlElement, resControlPanel);
			}else {
				log.warn("unsupported case11");
			}
		}
	}

	public void parseOpening(IfcOpeningElement opening, Resource resParent) {
		Iterator<IfcRelFillsElement> relFillsElementIterator = opening.getHasFillings().iterator();
		while(relFillsElementIterator.hasNext()) {
			IfcRelFillsElement relFillsElement = relFillsElementIterator.next();
			if(relFillsElement.getRelatedBuildingElement() instanceof IfcDoor) {
				IfcDoor ifcDoor = (IfcDoor) relFillsElement.getRelatedBuildingElement();
				Resource resDoor = rdfModel.createResource(rdfModel.getNsPrefixURI("om") + "Element_" + ifcDoor.getExpressId());
				resDoor.addLiteral(RDFS.label, ResourceFactory.createStringLiteral(  ifcDoor.getName() != null ? ifcDoor.getName().getValue() : "Undefined"  ));
				resDoor.addProperty(RDF.type, ResourceFactory.createResource( rdfModel.getNsPrefixURI("bot") + "Element"));
				resDoor.addProperty(RDF.type, ResourceFactory.createResource( rdfModel.getNsPrefixURI("beo") + "Door"));	
				resDoor.addLiteral(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("props") + "hasCompressedGuid"), ResourceFactory.createStringLiteral( ifcDoor.getGlobalId().getValue() ));
				parsePsets(ifcDoor, resDoor);
				// upd parent
				resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("bot") + "hasSubElement"), resDoor);													
			}else if(relFillsElement.getRelatedBuildingElement() instanceof IfcWindow) {				
				IfcWindow ifcWindow = (IfcWindow) relFillsElement.getRelatedBuildingElement();
				Resource resWindow = rdfModel.createResource(rdfModel.getNsPrefixURI("om") + "Element_" + ifcWindow.getExpressId());
				resWindow.addLiteral(RDFS.label, ResourceFactory.createStringLiteral(  ifcWindow.getName() != null ? ifcWindow.getName().getValue() : "Undefined"  ));
				resWindow.addProperty(RDF.type, ResourceFactory.createResource( rdfModel.getNsPrefixURI("bot") + "Element"));
				resWindow.addProperty(RDF.type, ResourceFactory.createResource( rdfModel.getNsPrefixURI("beo") + "Window"));
				resWindow.addLiteral(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("props") + "hasCompressedGuid"), ResourceFactory.createStringLiteral( ifcWindow.getGlobalId().getValue() ));
				parsePsets(ifcWindow, resWindow);
				// upd parent
				resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("bot") + "hasSubElement"), resWindow);													
			}else {
				log.warn("unsupported case");
			}
		}
	}

	void parsePsets(IfcProduct ifcProduct, Resource resProduct) {
		Iterator<IfcRelDefinesByProperties> windowRelDefinesByProperties = ifcProduct.getIsDefinedBy().iterator();
		while(windowRelDefinesByProperties.hasNext()) {
			IfcRelDefinesByProperties relDefinesByProperties = windowRelDefinesByProperties.next();
			if(relDefinesByProperties.getRelatingPropertyDefinition() instanceof IfcPropertySet) {
				IfcPropertySet propertySet = (IfcPropertySet) relDefinesByProperties.getRelatingPropertyDefinition();
				Iterator<IfcProperty> propertiesIterator = propertySet.getHasProperties().iterator();
				while(propertiesIterator.hasNext()) {
					IfcProperty ifcProperty = propertiesIterator.next();
					if(ifcProperty instanceof IfcPropertySingleValue) {
						IfcPropertySingleValue propertySignleValue = (IfcPropertySingleValue) ifcProperty;
						if(propertySignleValue.getName().getValue().equalsIgnoreCase("Ufactor")) {
							IfcValue ifcValue = propertySignleValue.getNominalValue();
							if(ifcValue instanceof IfcReal) {
								IfcReal ifcMeasure = (IfcReal) ifcValue;
								resProduct.addLiteral(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("props") + "heatTransferCoefficientU"),  ResourceFactory.createTypedLiteral(ifcMeasure.getValue()));
							}									
						}else if(propertySignleValue.getName().getValue().equalsIgnoreCase("SolarHeatGainCoefficient")) {
							IfcValue ifcValue = propertySignleValue.getNominalValue();
							if(ifcValue instanceof IfcReal) {
								IfcReal ifcMeasure = (IfcReal) ifcValue;
								resProduct.addLiteral(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("props") + "solarHeatGainCoefficient"),  ResourceFactory.createTypedLiteral(ifcMeasure.getValue()));
							}
						}else if(propertySignleValue.getName().getValue().equalsIgnoreCase("VisibleTransmittance")) {
							IfcValue ifcValue = propertySignleValue.getNominalValue();
							if(ifcValue instanceof IfcReal) {
								IfcReal ifcMeasure = (IfcReal) ifcValue;
								resProduct.addLiteral(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("props") + "visualLightTransmittance"),  ResourceFactory.createTypedLiteral(ifcMeasure.getValue()));
							}
						//}else if(propertySignleValue.getName().getValue().equalsIgnoreCase("Area")) {
						//IfcValue ifcValue = propertySignleValue.getNominalValue();
						//if(ifcValue instanceof IfcAreaMeasure) {
						//IfcAreaMeasure ifcMeasure = (IfcAreaMeasure) ifcValue;
						//resProduct.addLiteral(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("props") + "hasArea"),  ResourceFactory.createTypedLiteral(ifcMeasure.getValue()));
						//}
						//}else if(propertySignleValue.getName().getValue().equalsIgnoreCase("Volume")) {
						//IfcValue ifcValue = propertySignleValue.getNominalValue();
						//if(ifcValue instanceof IfcVolumeMeasure) {
						//IfcVolumeMeasure ifcMeasure = (IfcVolumeMeasure) ifcValue;
						//resProduct.addLiteral(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("props") + "hasVolume"),  ResourceFactory.createTypedLiteral(ifcMeasure.getValue()));
						//}
						}else if(propertySignleValue.getName().getValue().equalsIgnoreCase("Serial")) {
							IfcValue ifcValue = propertySignleValue.getNominalValue();
							if(ifcValue instanceof IfcIdentifier) {
								IfcIdentifier ifcIdentifier = (IfcIdentifier) ifcValue;
								resProduct.addLiteral(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("s4ener") + "hasSerialNumber"),  ResourceFactory.createStringLiteral(ifcIdentifier.getValue()));
							}
						}else if(propertySignleValue.getName().getValue().equalsIgnoreCase("COP")) {
							IfcValue ifcValue = propertySignleValue.getNominalValue();
							if(ifcValue instanceof IfcReal) {
								IfcReal ifcMeasure = (IfcReal) ifcValue;
								resProduct.addLiteral(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("props") + "hasCOP"),  ResourceFactory.createTypedLiteral(ifcMeasure.getValue()));
							}
						}else if(propertySignleValue.getName().getValue().equalsIgnoreCase("EER")) {
							IfcValue ifcValue = propertySignleValue.getNominalValue();
							if(ifcValue instanceof IfcReal) {
								IfcReal ifcMeasure = (IfcReal) ifcValue;
								resProduct.addLiteral(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("props") + "hasEER"),  ResourceFactory.createTypedLiteral(ifcMeasure.getValue()));
							}
						}
					}
				}
			}else {
				log.warn("unsupported case");
			}
		}
	}
	
	
	public String getSerial(IfcProduct ifcProduct) {
		Iterator<IfcRelDefinesByProperties> windowRelDefinesByProperties = ifcProduct.getIsDefinedBy().iterator();
		while(windowRelDefinesByProperties.hasNext()) {
			IfcRelDefinesByProperties relDefinesByProperties = windowRelDefinesByProperties.next();
			if(relDefinesByProperties.getRelatingPropertyDefinition() instanceof IfcPropertySet) {
				IfcPropertySet propertySet = (IfcPropertySet) relDefinesByProperties.getRelatingPropertyDefinition();
				Iterator<IfcProperty> propertiesIterator = propertySet.getHasProperties().iterator();
				while(propertiesIterator.hasNext()) {
					IfcProperty ifcProperty = propertiesIterator.next();
					if(ifcProperty instanceof IfcPropertySingleValue) {
						IfcPropertySingleValue propertySignleValue = (IfcPropertySingleValue) ifcProperty;
						 if(propertySignleValue.getName().getValue().equalsIgnoreCase("Serial")) {
							IfcValue ifcValue = propertySignleValue.getNominalValue();
							if(ifcValue instanceof IfcText) {
								IfcText ifcText = (IfcText) ifcValue;
								return ifcText.getValue();
							}
						}
					}
				}
			}else {
				log.warn("unsupported case");
			}
		}
		return null;
	}
	
}