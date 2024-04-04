package rdfconverter;

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
import gr.tuc.ifc4.IfcValve;
import gr.tuc.ifc4.IfcPump;
import gr.tuc.ifc4.IfcAirTerminalBox;
import gr.tuc.ifc4.IfcBoiler;
import gr.tuc.ifc4.IfcCompressor;
import gr.tuc.ifc4.IfcChiller;
import gr.tuc.ifc4.IfcCoil;
import gr.tuc.ifc4.IfcDamper;
import gr.tuc.ifc4.IfcFan;
import gr.tuc.ifc4.IfcHeatExchanger;

@Service
public class BIMtoRDFconverter {

	private static Logger log = LoggerFactory.getLogger(BIMtoRDFconverter.class);

	private Model rdfModel;

	public BIMtoRDFconverter() {
		log.info("Knowledge Graph Generator");
	}

	public Model ifcConvert(IfcModel ifcModel) {
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
		if (projectIterator.hasNext()) {
			IfcProject ifcProject = projectIterator.next();
			parseProject(ifcProject);
		}
		return rdfModel;
	}

	private void parseProject(IfcProject ifcProject) {
		Iterator<IfcRelAggregates> projectRelAggregatesIterator = ifcProject.getIsDecomposedBy().iterator();
		while (projectRelAggregatesIterator.hasNext()) {
			IfcRelAggregates projectRelAggregates = projectRelAggregatesIterator.next();
			List<IfcObjectDefinition> projectRelatedObjectList = projectRelAggregates.getRelatedObjects();
			for (IfcObjectDefinition projectRelatedObject : projectRelatedObjectList) {
				if (projectRelatedObject instanceof IfcSite) {
					IfcSite ifcSite = (IfcSite) projectRelatedObject;
					//
					Resource resSiteIfc = rdfModel.createResource();
					resSiteIfc.addProperty(RDF.type,
							ResourceFactory.createResource(rdfModel.getNsPrefixURI("ref") + "IFCReference"));
					resSiteIfc.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"),
							ifcSite.getName() != null ? ifcSite.getName().getValue() : "Undefined");
					resSiteIfc.addProperty(
							ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"),
							ResourceFactory.createStringLiteral(ifcSite.getGlobalId().getValue()));
					//
					Resource resSite = rdfModel
							.createResource(rdfModel.getNsPrefixURI("om") + "Site_" + ifcSite.getExpressId());
					resSite.addProperty(RDF.type,
							ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Site"));
					resSite.addProperty(
							ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
							resSiteIfc);
					//
					Iterator<IfcRelAggregates> siteRelAggregatesIterator = ifcSite.getIsDecomposedBy().iterator();
					while (siteRelAggregatesIterator.hasNext()) {
						IfcRelAggregates siteRelAggregates = siteRelAggregatesIterator.next();
						List<IfcObjectDefinition> siteRelatedObjectList = siteRelAggregates.getRelatedObjects();
						for (IfcObjectDefinition siteRelatedObject : siteRelatedObjectList) {
							if (siteRelatedObject instanceof IfcBuilding) {
								IfcBuilding building = (IfcBuilding) siteRelatedObject;
								parseBuilding(building, resSite);
							} else {
								log.warn("unsupported case");
							}
						}
					}
				} else if (projectRelatedObject instanceof IfcBuilding) {
					IfcBuilding building = (IfcBuilding) projectRelatedObject;
					parseBuilding(building, null);
				} else {
					log.warn("usupported case");
				}
			}
		}
	}

	private void parseBuilding(IfcBuilding ifcBuilding, Resource resSite) {
		//
		Resource resBuildingIfcRef = rdfModel.createResource();
		resBuildingIfcRef.addProperty(RDF.type,
				ResourceFactory.createResource(rdfModel.getNsPrefixURI("ref") + "IFCReference"));
		resBuildingIfcRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"),
				ifcBuilding.getName() != null ? ifcBuilding.getName().getValue() : "Undefined");
		resBuildingIfcRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"),
				ResourceFactory.createStringLiteral(ifcBuilding.getGlobalId().getValue()));
		//
		Resource resBuilding = rdfModel
				.createResource(rdfModel.getNsPrefixURI("om") + "Building_" + ifcBuilding.getExpressId());
		resBuilding.addProperty(RDF.type,
				ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Building"));
		resBuilding.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
				resBuildingIfcRef);
		//
		//
		//
		// update parent
		if (resSite != null) {
			resSite.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPart"),
					resBuilding);
		}
		// using containedInSpatialStructure
		Iterator<IfcRelContainedInSpatialStructure> buildingContainedInSpatialStructureIterator = ifcBuilding
				.getContainsElements().iterator();
		while (buildingContainedInSpatialStructureIterator.hasNext()) {
			IfcRelContainedInSpatialStructure relContainedInSpatialStructure = buildingContainedInSpatialStructureIterator
					.next();
			for (IfcProduct product : relContainedInSpatialStructure.getRelatedElements()) {
				parseProduct(product, resBuilding);
			}
		}
		// using decomposedBy
		Iterator<IfcRelAggregates> buildingRelAggregatesIterator = ifcBuilding.getIsDecomposedBy().iterator();
		while (buildingRelAggregatesIterator.hasNext()) {
			IfcRelAggregates buildingRelAggregates = buildingRelAggregatesIterator.next();
			List<IfcObjectDefinition> buildingRelatedObjectList = buildingRelAggregates.getRelatedObjects();
			for (IfcObjectDefinition builidngRelatedObject : buildingRelatedObjectList) {
				if (builidngRelatedObject instanceof IfcBuildingStorey) {
					IfcBuildingStorey ifcBuildingStorey = (IfcBuildingStorey) builidngRelatedObject;
					//
					Resource resBuildingStoreyIfcRef = rdfModel.createResource();
					resBuildingStoreyIfcRef.addProperty(
							ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"),
							ifcBuilding.getName() != null ? ifcBuilding.getName().getValue() : "Undefined");
					resBuildingStoreyIfcRef.addProperty(
							ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"),
							ResourceFactory.createStringLiteral(ifcBuilding.getGlobalId().getValue()));
					//
					Resource resStorey = rdfModel.createResource(
							rdfModel.getNsPrefixURI("om") + "BuildingStorey_" + ifcBuildingStorey.getExpressId());
					resStorey.addProperty(RDF.type,
							ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Storey"));
					resStorey.addProperty(
							ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
							resBuildingStoreyIfcRef);
					//
					// parent resource
					resBuilding.addProperty(
							ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPart"), resStorey);
					// using decomposedBy
					Iterator<IfcRelAggregates> buildingStoreyRelAggregatesIterator = ifcBuildingStorey
							.getIsDecomposedBy().iterator();
					while (buildingStoreyRelAggregatesIterator.hasNext()) {
						IfcRelAggregates buildingStoreyRelAggregates = buildingStoreyRelAggregatesIterator.next();
						List<IfcObjectDefinition> buildingStoreyRelatedObjectList = buildingStoreyRelAggregates
								.getRelatedObjects();
						for (IfcObjectDefinition buildingStoreyRelatedObject : buildingStoreyRelatedObjectList) {
							if (buildingStoreyRelatedObject instanceof IfcProduct) {
								IfcProduct product = (IfcProduct) buildingStoreyRelatedObject;
								parseProduct(product, resStorey);
							} else {
								log.warn("unsupported case");
							}
						}
					}
					// using containedInSpatialStructure
					Iterator<IfcRelContainedInSpatialStructure> buildingStoreyContainedInSpatialStructureIterator = ifcBuildingStorey
							.getContainsElements().iterator();
					while (buildingStoreyContainedInSpatialStructureIterator.hasNext()) {
						IfcRelContainedInSpatialStructure relContainedInSpatialStructure = buildingStoreyContainedInSpatialStructureIterator
								.next();
						for (IfcProduct product : relContainedInSpatialStructure.getRelatedElements()) {
							parseProduct(product, resStorey);
						}
					}
				} else {
					log.warn("unsupported case");
				}
			}
		}
	}

	private void parseProduct(IfcProduct product, Resource resParent) {
		if (product instanceof IfcSpace) {
			IfcSpace ifcSpace = (IfcSpace) product;
			//
			Resource resSpaceIfcRef = rdfModel.createResource();
			resSpaceIfcRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"),
					ifcSpace.getName() != null ? ifcSpace.getName().getValue() : "Undefined");
			resSpaceIfcRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"),
					ResourceFactory.createStringLiteral(ifcSpace.getGlobalId().getValue()));
			//
			Resource resSpace = rdfModel
					.createResource(rdfModel.getNsPrefixURI("om") + "Space_" + ifcSpace.getExpressId());
			resSpace.addProperty(RDF.type, ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Space"));
			resSpace.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "BuildingSpace"));
			resSpace.addProperty(
					ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
					resSpaceIfcRef);
			//
			resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPart"),
					resSpace);

			//
			// properties
			parsePsets(ifcSpace, resSpace);
			//
			// zones
			Iterator<IfcRelAssigns> spaceAssignmentIterator = ifcSpace.getHasAssignments().iterator();
			while (spaceAssignmentIterator.hasNext()) {
				IfcRelAssigns relAssigns = spaceAssignmentIterator.next();
				if (relAssigns instanceof IfcRelAssignsToGroup) {
					IfcRelAssignsToGroup relAssignsToGroup = (IfcRelAssignsToGroup) relAssigns;
					if (relAssignsToGroup.getRelatingGroup() instanceof IfcZone) {
						IfcZone ifcZone = (IfcZone) relAssignsToGroup.getRelatingGroup();
						//
						//
						Resource resZoneIfcRef = rdfModel
								.createResource(rdfModel.getNsPrefixURI("om") + "ZoneRef_" + ifcZone.getExpressId());
						resZoneIfcRef.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("ref") + "IFCReference"));
						resZoneIfcRef.addProperty(
								ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"),
								ifcZone.getName() != null ? ifcZone.getName().getValue() : "Undefined");
						resZoneIfcRef.addProperty(
								ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"),
								ResourceFactory.createStringLiteral(ifcZone.getGlobalId().getValue()));
						//
						//
						Resource resZone = rdfModel
								.createResource(rdfModel.getNsPrefixURI("om") + "Zone_" + ifcZone.getExpressId());
						resZone.addProperty(RDF.type,
								ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Zone"));
						resZone.addProperty(
								ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
								resZoneIfcRef);
						resZone.addProperty(
								ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasPart"), resSpace);
						//
					} else {
						log.warn("unsupported case");
					}
				} else {
					log.warn("unsupported case");
				}
			}
			// products included in the space
			Iterator<IfcRelContainedInSpatialStructure> buildingContainedInSpatialStructureIterator = ifcSpace
					.getContainsElements().iterator();
			while (buildingContainedInSpatialStructureIterator.hasNext()) {
				IfcRelContainedInSpatialStructure relContainedInSpatialStructure = buildingContainedInSpatialStructureIterator
						.next();
				for (IfcProduct spaceProduct : relContainedInSpatialStructure.getRelatedElements()) {
					parseProduct(spaceProduct, resSpace);
				}
			}

		} else if (product instanceof IfcUnitaryEquipment) {
			IfcUnitaryEquipment ifcUnitaryEquipment = (IfcUnitaryEquipment) product;
			if (ifcUnitaryEquipment.getPredefinedType().equals(IfcUnitaryEquipmentTypeEnum.SPLITSYSTEM)) {
				//
				Resource resSplitSystemRef = rdfModel.createResource();
				resSplitSystemRef.addProperty(
						ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"),
						ifcUnitaryEquipment.getName() != null ? ifcUnitaryEquipment.getName().getValue() : "Undefined");
				resSplitSystemRef.addProperty(
						ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"),
						ResourceFactory.createStringLiteral(ifcUnitaryEquipment.getGlobalId().getValue()));
				//
				Resource resSplitSystem = rdfModel.createResource(
						rdfModel.getNsPrefixURI("om") + getSerial(product) + "_Equipment");
				resSplitSystem.addProperty(RDF.type,
						ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Terminal_Unit"));
				resSplitSystem.addProperty(RDF.type,
						ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "FlowTerminal"));
				resSplitSystem.addProperty(
						ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
						resSplitSystemRef);
				resSplitSystem.addProperty(
						ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasLocation"), resParent);
				//
				// parent
				resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isLocationOf"),
						resSplitSystem);
				resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("s4bldg") + "contains"),
						resSplitSystem);
				//
				Iterator<IfcRelContainedInSpatialStructure> containedInSpatialStructureIterator = ifcUnitaryEquipment
						.getContainedInStructure().iterator();
				while (containedInSpatialStructureIterator.hasNext()) {
					IfcRelContainedInSpatialStructure relContainedInSpatialStructure = containedInSpatialStructureIterator
							.next();
					IfcSpatialElement spatialElement = relContainedInSpatialStructure.getRelatingStructure();
					if (spatialElement instanceof IfcSpace) {
						IfcSpace ifcSpace = (IfcSpace) spatialElement;
						Iterator<IfcRelAssigns> relAssignIterator = ifcSpace.getHasAssignments().iterator();
						while (relAssignIterator.hasNext()) {
							IfcRelAssigns relAssigns = relAssignIterator.next();
							if (relAssigns instanceof IfcRelAssignsToGroup) {
								IfcRelAssignsToGroup relAssignsToGroup = (IfcRelAssignsToGroup) relAssigns;
								if (relAssignsToGroup.getRelatingGroup() instanceof IfcZone) {
									IfcZone ifcZone = (IfcZone) relAssignsToGroup.getRelatingGroup();
									Resource resZone = rdfModel.createResource(
											rdfModel.getNsPrefixURI("om") + "Zone_" + ifcZone.getExpressId());
									resSplitSystem.addProperty(
											ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "feeds"),
											resZone);
								}
							}
						}
					} else {
						log.warn("unsupported case");
					}
				}
				// add psets
				parsePsets(ifcUnitaryEquipment, resSplitSystem);

			} else {
				log.warn("unsupported case");
			}

		} else if (product instanceof IfcUnitaryEquipment) {
			IfcUnitaryEquipment ifcUnitaryEquipment = (IfcUnitaryEquipment) product;
			if (ifcUnitaryEquipment.getPredefinedType().equals(IfcUnitaryEquipmentTypeEnum.AIRHANDLER)) {
				//
				Resource resAHURef = rdfModel.createResource();
				resAHURef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"),
						ifcUnitaryEquipment.getName() != null ? ifcUnitaryEquipment.getName().getValue() : "Undefined");
				resAHURef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"),
						ResourceFactory.createStringLiteral(ifcUnitaryEquipment.getGlobalId().getValue()));
				//
				Resource resAHU = rdfModel.createResource(
						rdfModel.getNsPrefixURI("om") + getSerial(product) + "_Equipment");
				resAHU.addProperty(RDF.type,
						ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Air_Handler_Unit"));
				resAHU.addProperty(RDF.type,
						ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "DistributionDevice"));
				resAHU.addProperty(
						ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
						resAHURef);
				resAHU.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasLocation"),
						resParent);
				//
				// parent
				resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isLocationOf"),
						resAHU);
				resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("s4bldg") + "contains"),
						resAHU);
				//
				Iterator<IfcRelContainedInSpatialStructure> containedInSpatialStructureIterator = ifcUnitaryEquipment
						.getContainedInStructure().iterator();
				while (containedInSpatialStructureIterator.hasNext()) {
					IfcRelContainedInSpatialStructure relContainedInSpatialStructure = containedInSpatialStructureIterator
							.next();
					IfcSpatialElement spatialElement = relContainedInSpatialStructure.getRelatingStructure();
					if (spatialElement instanceof IfcSpace) {
						IfcSpace ifcSpace = (IfcSpace) spatialElement;
						Iterator<IfcRelAssigns> relAssignIterator = ifcSpace.getHasAssignments().iterator();
						while (relAssignIterator.hasNext()) {
							IfcRelAssigns relAssigns = relAssignIterator.next();
							if (relAssigns instanceof IfcRelAssignsToGroup) {
								IfcRelAssignsToGroup relAssignsToGroup = (IfcRelAssignsToGroup) relAssigns;
								if (relAssignsToGroup.getRelatingGroup() instanceof IfcZone) {
									IfcZone ifcZone = (IfcZone) relAssignsToGroup.getRelatingGroup();
									Resource resZone = rdfModel.createResource(
											rdfModel.getNsPrefixURI("om") + "Zone_" + ifcZone.getExpressId());
									resAHU.addProperty(
											ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "feeds"),
											resZone);
								}
							}
						}
					} else {
						log.warn("unsupported case");
					}
				}
				// add psets
				parsePsets(ifcUnitaryEquipment, resAHU);

			} else {
				log.warn("unsupported case");
			}

		} else if (product instanceof IfcValve) {
			IfcValve ifcValve = (IfcValve) product;
			//
			Resource resValveRef = rdfModel.createResource();
			resValveRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"),
					ifcValve.getName() != null ? ifcValve.getName().getValue() : "Undefined");
			resValveRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"),
					ResourceFactory.createStringLiteral(ifcValve.getGlobalId().getValue()));
			//
			Resource resValve = rdfModel
					.createResource(rdfModel.getNsPrefixURI("om") + getSerial(product) + "_Equipment");
			resValve.addProperty(RDF.type, ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Valve"));
			resValve.addProperty(RDF.type, ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "Valve"));
			resValve.addProperty(
					ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
					resValveRef);
			resValve.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasLocation"),
					resParent);
			//
			// parent
			resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isLocationOf"),
					resValve);
			resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("s4bldg") + "contains"),
					resValve);
			//
			Iterator<IfcRelContainedInSpatialStructure> containedInSpatialStructureIterator = ifcValve
					.getContainedInStructure().iterator();
			while (containedInSpatialStructureIterator.hasNext()) {
				IfcRelContainedInSpatialStructure relContainedInSpatialStructure = containedInSpatialStructureIterator
						.next();
				IfcSpatialElement spatialElement = relContainedInSpatialStructure.getRelatingStructure();
				if (spatialElement instanceof IfcSpace) {
					IfcSpace ifcSpace = (IfcSpace) spatialElement;
					Iterator<IfcRelAssigns> relAssignIterator = ifcSpace.getHasAssignments().iterator();
					while (relAssignIterator.hasNext()) {
						IfcRelAssigns relAssigns = relAssignIterator.next();
						if (relAssigns instanceof IfcRelAssignsToGroup) {
							IfcRelAssignsToGroup relAssignsToGroup = (IfcRelAssignsToGroup) relAssigns;
							if (relAssignsToGroup.getRelatingGroup() instanceof IfcZone) {
								IfcZone ifcZone = (IfcZone) relAssignsToGroup.getRelatingGroup();
								Resource resZone = rdfModel.createResource(
										rdfModel.getNsPrefixURI("om") + "Zone_" + ifcZone.getExpressId());
								resValve.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "feeds"),
										resZone);
							}
						}
					}
				} else {
					log.warn("unsupported case");
				}
			}
			// add psets
			parsePsets(ifcValve, resValve);

		} else if (product instanceof IfcPump) {
			IfcPump ifcPump = (IfcPump) product;
			//
			Resource resPumpRef = rdfModel.createResource();
			resPumpRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"),
					ifcPump.getName() != null ? ifcPump.getName().getValue() : "Undefined");
			resPumpRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"),
					ResourceFactory.createStringLiteral(ifcPump.getGlobalId().getValue()));
			//
			Resource resPump = rdfModel
					.createResource(rdfModel.getNsPrefixURI("om") + getSerial(product) + "_Equipment");
			resPump.addProperty(RDF.type, ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Pump"));
			resPump.addProperty(RDF.type, ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "Pump"));
			resPump.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
					resPumpRef);
			resPump.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasLocation"),
					resParent);
			//
			// parent
			resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isLocationOf"),
					resPump);
			resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("s4bldg") + "contains"),
					resPump);
			//
			Iterator<IfcRelContainedInSpatialStructure> containedInSpatialStructureIterator = ifcPump
					.getContainedInStructure().iterator();
			while (containedInSpatialStructureIterator.hasNext()) {
				IfcRelContainedInSpatialStructure relContainedInSpatialStructure = containedInSpatialStructureIterator
						.next();
				IfcSpatialElement spatialElement = relContainedInSpatialStructure.getRelatingStructure();
				if (spatialElement instanceof IfcSpace) {
					IfcSpace ifcSpace = (IfcSpace) spatialElement;
					Iterator<IfcRelAssigns> relAssignIterator = ifcSpace.getHasAssignments().iterator();
					while (relAssignIterator.hasNext()) {
						IfcRelAssigns relAssigns = relAssignIterator.next();
						if (relAssigns instanceof IfcRelAssignsToGroup) {
							IfcRelAssignsToGroup relAssignsToGroup = (IfcRelAssignsToGroup) relAssigns;
							if (relAssignsToGroup.getRelatingGroup() instanceof IfcZone) {
								IfcZone ifcZone = (IfcZone) relAssignsToGroup.getRelatingGroup();
								Resource resZone = rdfModel.createResource(
										rdfModel.getNsPrefixURI("om") + "Zone_" + ifcZone.getExpressId());
								resPump.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "feeds"),
										resZone);
							}
						}
					}
				} else {
					log.warn("unsupported case");
				}
			}
			// add psets
			parsePsets(ifcPump, resPump);

		} else if (product instanceof IfcAirTerminalBox) {
			IfcAirTerminalBox ifcAirTerminalBox = (IfcAirTerminalBox) product;
			//
			Resource resVAVRef = rdfModel.createResource();
			resVAVRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"),
					ifcAirTerminalBox.getName() != null ? ifcAirTerminalBox.getName().getValue() : "Undefined");
			resVAVRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"),
					ResourceFactory.createStringLiteral(ifcAirTerminalBox.getGlobalId().getValue()));
			//
			Resource resVAV = rdfModel
					.createResource(rdfModel.getNsPrefixURI("om") + getSerial(product) + "_Equipment");
			resVAV.addProperty(RDF.type, ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Pump"));
			resVAV.addProperty(RDF.type, ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "Pump"));
			resVAV.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
					resVAVRef);
			resVAV.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasLocation"),
					resParent);
			//
			// parent
			resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isLocationOf"),
					resVAV);
			resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("s4bldg") + "contains"),
					resVAV);
			//
			Iterator<IfcRelContainedInSpatialStructure> containedInSpatialStructureIterator = ifcAirTerminalBox
					.getContainedInStructure().iterator();
			while (containedInSpatialStructureIterator.hasNext()) {
				IfcRelContainedInSpatialStructure relContainedInSpatialStructure = containedInSpatialStructureIterator
						.next();
				IfcSpatialElement spatialElement = relContainedInSpatialStructure.getRelatingStructure();
				if (spatialElement instanceof IfcSpace) {
					IfcSpace ifcSpace = (IfcSpace) spatialElement;
					Iterator<IfcRelAssigns> relAssignIterator = ifcSpace.getHasAssignments().iterator();
					while (relAssignIterator.hasNext()) {
						IfcRelAssigns relAssigns = relAssignIterator.next();
						if (relAssigns instanceof IfcRelAssignsToGroup) {
							IfcRelAssignsToGroup relAssignsToGroup = (IfcRelAssignsToGroup) relAssigns;
							if (relAssignsToGroup.getRelatingGroup() instanceof IfcZone) {
								IfcZone ifcZone = (IfcZone) relAssignsToGroup.getRelatingGroup();
								Resource resZone = rdfModel.createResource(
										rdfModel.getNsPrefixURI("om") + "Zone_" + ifcZone.getExpressId());
								resVAV.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "feeds"),
										resZone);
							}
						}
					}
				} else {
					log.warn("unsupported case");
				}
			}
			// add psets
			parsePsets(ifcAirTerminalBox, resVAV);

		} else if (product instanceof IfcBoiler) {
			IfcBoiler ifcBoiler = (IfcBoiler) product;
			//
			Resource resBoilerRef = rdfModel.createResource();
			resBoilerRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"),
					ifcBoiler.getName() != null ? ifcBoiler.getName().getValue() : "Undefined");
			resBoilerRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"),
					ResourceFactory.createStringLiteral(ifcBoiler.getGlobalId().getValue()));
			//
			Resource resBoiler = rdfModel
					.createResource(rdfModel.getNsPrefixURI("om") + getSerial(product) + "_Equipment");
			resBoiler.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Boiler"));
			resBoiler.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "Boiler"));
			resBoiler.addProperty(
					ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
					resBoilerRef);
			resBoiler.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasLocation"),
					resParent);
			//
			// parent
			resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isLocationOf"),
					resBoiler);
			resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("s4bldg") + "contains"),
					resBoiler);
			//
			Iterator<IfcRelContainedInSpatialStructure> containedInSpatialStructureIterator = ifcBoiler
					.getContainedInStructure().iterator();
			while (containedInSpatialStructureIterator.hasNext()) {
				IfcRelContainedInSpatialStructure relContainedInSpatialStructure = containedInSpatialStructureIterator
						.next();
				IfcSpatialElement spatialElement = relContainedInSpatialStructure.getRelatingStructure();
				if (spatialElement instanceof IfcSpace) {
					IfcSpace ifcSpace = (IfcSpace) spatialElement;
					Iterator<IfcRelAssigns> relAssignIterator = ifcSpace.getHasAssignments().iterator();
					while (relAssignIterator.hasNext()) {
						IfcRelAssigns relAssigns = relAssignIterator.next();
						if (relAssigns instanceof IfcRelAssignsToGroup) {
							IfcRelAssignsToGroup relAssignsToGroup = (IfcRelAssignsToGroup) relAssigns;
							if (relAssignsToGroup.getRelatingGroup() instanceof IfcZone) {
								IfcZone ifcZone = (IfcZone) relAssignsToGroup.getRelatingGroup();
								Resource resZone = rdfModel.createResource(
										rdfModel.getNsPrefixURI("om") + "Zone_" + ifcZone.getExpressId());
								resBoiler.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "feeds"),
										resZone);
							}
						}
					}
				} else {
					log.warn("unsupported case");
				}
			}
			// add psets
			parsePsets(ifcBoiler, resBoiler);

		} else if (product instanceof IfcCompressor) {
			IfcCompressor ifcCompressor = (IfcCompressor) product;
			//
			Resource resCompressorRef = rdfModel.createResource();
			resCompressorRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"),
					ifcCompressor.getName() != null ? ifcCompressor.getName().getValue() : "Undefined");
			resCompressorRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"),
					ResourceFactory.createStringLiteral(ifcCompressor.getGlobalId().getValue()));
			//
			Resource resCompressor = rdfModel
					.createResource(rdfModel.getNsPrefixURI("om") + getSerial(product) + "_Equipment");
			resCompressor.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Pump"));
			resCompressor.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "Pump"));
			resCompressor.addProperty(
					ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
					resCompressorRef);
			resCompressor.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasLocation"),
					resParent);
			//
			// parent
			resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isLocationOf"),
					resCompressor);
			resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("s4bldg") + "contains"),
					resCompressor);
			//
			Iterator<IfcRelContainedInSpatialStructure> containedInSpatialStructureIterator = ifcCompressor
					.getContainedInStructure().iterator();
			while (containedInSpatialStructureIterator.hasNext()) {
				IfcRelContainedInSpatialStructure relContainedInSpatialStructure = containedInSpatialStructureIterator
						.next();
				IfcSpatialElement spatialElement = relContainedInSpatialStructure.getRelatingStructure();
				if (spatialElement instanceof IfcSpace) {
					IfcSpace ifcSpace = (IfcSpace) spatialElement;
					Iterator<IfcRelAssigns> relAssignIterator = ifcSpace.getHasAssignments().iterator();
					while (relAssignIterator.hasNext()) {
						IfcRelAssigns relAssigns = relAssignIterator.next();
						if (relAssigns instanceof IfcRelAssignsToGroup) {
							IfcRelAssignsToGroup relAssignsToGroup = (IfcRelAssignsToGroup) relAssigns;
							if (relAssignsToGroup.getRelatingGroup() instanceof IfcZone) {
								IfcZone ifcZone = (IfcZone) relAssignsToGroup.getRelatingGroup();
								Resource resZone = rdfModel.createResource(
										rdfModel.getNsPrefixURI("om") + "Zone_" + ifcZone.getExpressId());
								resCompressor.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "feeds"),
										resZone);
							}
						}
					}
				} else {
					log.warn("unsupported case");
				}
			}
			// add psets
			parsePsets(ifcCompressor, resCompressor);

		} else if (product instanceof IfcChiller) {
			IfcChiller ifcChiller = (IfcChiller) product;
			//
			Resource resChillerRef = rdfModel.createResource();
			resChillerRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"),
					ifcChiller.getName() != null ? ifcChiller.getName().getValue() : "Undefined");
			resChillerRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"),
					ResourceFactory.createStringLiteral(ifcChiller.getGlobalId().getValue()));
			//
			Resource resChiller = rdfModel
					.createResource(rdfModel.getNsPrefixURI("om") + getSerial(product) + "_Equipment");
			resChiller.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Chiller"));
			resChiller.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "Chiller"));
			resChiller.addProperty(
					ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
					resChillerRef);
			resChiller.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasLocation"),
					resParent);
			//
			// parent
			resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isLocationOf"),
					resChiller);
			resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("s4bldg") + "contains"),
					resChiller);
			//
			Iterator<IfcRelContainedInSpatialStructure> containedInSpatialStructureIterator = ifcChiller
					.getContainedInStructure().iterator();
			while (containedInSpatialStructureIterator.hasNext()) {
				IfcRelContainedInSpatialStructure relContainedInSpatialStructure = containedInSpatialStructureIterator
						.next();
				IfcSpatialElement spatialElement = relContainedInSpatialStructure.getRelatingStructure();
				if (spatialElement instanceof IfcSpace) {
					IfcSpace ifcSpace = (IfcSpace) spatialElement;
					Iterator<IfcRelAssigns> relAssignIterator = ifcSpace.getHasAssignments().iterator();
					while (relAssignIterator.hasNext()) {
						IfcRelAssigns relAssigns = relAssignIterator.next();
						if (relAssigns instanceof IfcRelAssignsToGroup) {
							IfcRelAssignsToGroup relAssignsToGroup = (IfcRelAssignsToGroup) relAssigns;
							if (relAssignsToGroup.getRelatingGroup() instanceof IfcZone) {
								IfcZone ifcZone = (IfcZone) relAssignsToGroup.getRelatingGroup();
								Resource resZone = rdfModel.createResource(
										rdfModel.getNsPrefixURI("om") + "Zone_" + ifcZone.getExpressId());
								resChiller.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "feeds"),
										resZone);
							}
						}
					}
				} else {
					log.warn("unsupported case");
				}
			}
			// add psets
			parsePsets(ifcChiller, resChiller);

		} else if (product instanceof IfcCoil) {
			IfcCoil ifcCoil = (IfcCoil) product;
			//
			Resource resCoilRef = rdfModel.createResource();
			resCoilRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"),
					ifcCoil.getName() != null ? ifcCoil.getName().getValue() : "Undefined");
			resCoilRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"),
					ResourceFactory.createStringLiteral(ifcCoil.getGlobalId().getValue()));
			//
			Resource resCoil = rdfModel
					.createResource(rdfModel.getNsPrefixURI("om") + getSerial(product) + "_Equipment");
			resCoil.addProperty(RDF.type, ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Coil"));
			resCoil.addProperty(RDF.type, ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "Coil"));
			resCoil.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
					resCoilRef);
			resCoil.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasLocation"),
					resParent);
			//
			// parent
			resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isLocationOf"),
					resCoil);
			resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("s4bldg") + "contains"),
					resCoil);
			//
			Iterator<IfcRelContainedInSpatialStructure> containedInSpatialStructureIterator = ifcCoil
					.getContainedInStructure().iterator();
			while (containedInSpatialStructureIterator.hasNext()) {
				IfcRelContainedInSpatialStructure relContainedInSpatialStructure = containedInSpatialStructureIterator
						.next();
				IfcSpatialElement spatialElement = relContainedInSpatialStructure.getRelatingStructure();
				if (spatialElement instanceof IfcSpace) {
					IfcSpace ifcSpace = (IfcSpace) spatialElement;
					Iterator<IfcRelAssigns> relAssignIterator = ifcSpace.getHasAssignments().iterator();
					while (relAssignIterator.hasNext()) {
						IfcRelAssigns relAssigns = relAssignIterator.next();
						if (relAssigns instanceof IfcRelAssignsToGroup) {
							IfcRelAssignsToGroup relAssignsToGroup = (IfcRelAssignsToGroup) relAssigns;
							if (relAssignsToGroup.getRelatingGroup() instanceof IfcZone) {
								IfcZone ifcZone = (IfcZone) relAssignsToGroup.getRelatingGroup();
								Resource resZone = rdfModel.createResource(
										rdfModel.getNsPrefixURI("om") + "Zone_" + ifcZone.getExpressId());
								resCoil.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "feeds"),
										resZone);
							}
						}
					}
				} else {
					log.warn("unsupported case");
				}
			}
			// add psets
			parsePsets(ifcCoil, resCoil);

		} else if (product instanceof IfcDamper) {
			IfcDamper ifcDamper = (IfcDamper) product;
			//
			Resource resDamperRef = rdfModel.createResource();
			resDamperRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"),
					ifcDamper.getName() != null ? ifcDamper.getName().getValue() : "Undefined");
			resDamperRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"),
					ResourceFactory.createStringLiteral(ifcDamper.getGlobalId().getValue()));
			//
			Resource resDamper = rdfModel
					.createResource(rdfModel.getNsPrefixURI("om") + getSerial(product) + "_Equipment");
			resDamper.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Damper"));
			resDamper.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "Damper"));
			resDamper.addProperty(
					ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
					resDamperRef);
			resDamper.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasLocation"),
					resParent);
			//
			// parent
			resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isLocationOf"),
					resDamper);
			resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("s4bldg") + "contains"),
					resDamper);
			//
			Iterator<IfcRelContainedInSpatialStructure> containedInSpatialStructureIterator = ifcDamper
					.getContainedInStructure().iterator();
			while (containedInSpatialStructureIterator.hasNext()) {
				IfcRelContainedInSpatialStructure relContainedInSpatialStructure = containedInSpatialStructureIterator
						.next();
				IfcSpatialElement spatialElement = relContainedInSpatialStructure.getRelatingStructure();
				if (spatialElement instanceof IfcSpace) {
					IfcSpace ifcSpace = (IfcSpace) spatialElement;
					Iterator<IfcRelAssigns> relAssignIterator = ifcSpace.getHasAssignments().iterator();
					while (relAssignIterator.hasNext()) {
						IfcRelAssigns relAssigns = relAssignIterator.next();
						if (relAssigns instanceof IfcRelAssignsToGroup) {
							IfcRelAssignsToGroup relAssignsToGroup = (IfcRelAssignsToGroup) relAssigns;
							if (relAssignsToGroup.getRelatingGroup() instanceof IfcZone) {
								IfcZone ifcZone = (IfcZone) relAssignsToGroup.getRelatingGroup();
								Resource resZone = rdfModel.createResource(
										rdfModel.getNsPrefixURI("om") + "Zone_" + ifcZone.getExpressId());
								resDamper.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "feeds"),
										resZone);
							}
						}
					}
				} else {
					log.warn("unsupported case");
				}
			}
			// add psets
			parsePsets(ifcDamper, resDamper);

		} else if (product instanceof IfcFan) {
			IfcFan ifcFan = (IfcFan) product;
			//
			Resource resFanRef = rdfModel.createResource();
			resFanRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"),
					ifcFan.getName() != null ? ifcFan.getName().getValue() : "Undefined");
			resFanRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"),
					ResourceFactory.createStringLiteral(ifcFan.getGlobalId().getValue()));
			//
			Resource resFan = rdfModel
					.createResource(rdfModel.getNsPrefixURI("om") + getSerial(product) + "_Equipment");
			resFan.addProperty(RDF.type, ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Fan"));
			resFan.addProperty(RDF.type, ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "Fan"));
			resFan.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
					resFanRef);
			resFan.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasLocation"),
					resParent);
			//
			// parent
			resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isLocationOf"),
					resFan);
			resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("s4bldg") + "contains"),
					resFan);
			//
			Iterator<IfcRelContainedInSpatialStructure> containedInSpatialStructureIterator = ifcFan
					.getContainedInStructure().iterator();
			while (containedInSpatialStructureIterator.hasNext()) {
				IfcRelContainedInSpatialStructure relContainedInSpatialStructure = containedInSpatialStructureIterator
						.next();
				IfcSpatialElement spatialElement = relContainedInSpatialStructure.getRelatingStructure();
				if (spatialElement instanceof IfcSpace) {
					IfcSpace ifcSpace = (IfcSpace) spatialElement;
					Iterator<IfcRelAssigns> relAssignIterator = ifcSpace.getHasAssignments().iterator();
					while (relAssignIterator.hasNext()) {
						IfcRelAssigns relAssigns = relAssignIterator.next();
						if (relAssigns instanceof IfcRelAssignsToGroup) {
							IfcRelAssignsToGroup relAssignsToGroup = (IfcRelAssignsToGroup) relAssigns;
							if (relAssignsToGroup.getRelatingGroup() instanceof IfcZone) {
								IfcZone ifcZone = (IfcZone) relAssignsToGroup.getRelatingGroup();
								Resource resZone = rdfModel.createResource(
										rdfModel.getNsPrefixURI("om") + "Zone_" + ifcZone.getExpressId());
								resFan.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "feeds"),
										resZone);
							}
						}
					}
				} else {
					log.warn("unsupported case");
				}
			}
			// add psets
			parsePsets(ifcFan, resFan);

		} else if (product instanceof IfcHeatExchanger) {
			IfcHeatExchanger ifcHeatExchanger = (IfcHeatExchanger) product;
			//
			Resource resHeatExchangerRef = rdfModel.createResource();
			resHeatExchangerRef.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"),
					ifcHeatExchanger.getName() != null ? ifcHeatExchanger.getName().getValue() : "Undefined");
			resHeatExchangerRef.addProperty(
					ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"),
					ResourceFactory.createStringLiteral(ifcHeatExchanger.getGlobalId().getValue()));
			//
			Resource resHeatExchanger = rdfModel
					.createResource(rdfModel.getNsPrefixURI("om") + getSerial(product) + "_Equipment");
			resHeatExchanger.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Heat_Exchanger"));
			resHeatExchanger.addProperty(RDF.type,
					ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "HeatExchanger"));
			resHeatExchanger.addProperty(
					ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
					resHeatExchangerRef);
			resHeatExchanger.addProperty(
					ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasLocation"), resParent);
			//
			// parent
			resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isLocationOf"),
					resHeatExchanger);
			resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("s4bldg") + "contains"),
					resHeatExchanger);
			//
			Iterator<IfcRelContainedInSpatialStructure> containedInSpatialStructureIterator = ifcHeatExchanger
					.getContainedInStructure().iterator();
			while (containedInSpatialStructureIterator.hasNext()) {
				IfcRelContainedInSpatialStructure relContainedInSpatialStructure = containedInSpatialStructureIterator
						.next();
				IfcSpatialElement spatialElement = relContainedInSpatialStructure.getRelatingStructure();
				if (spatialElement instanceof IfcSpace) {
					IfcSpace ifcSpace = (IfcSpace) spatialElement;
					Iterator<IfcRelAssigns> relAssignIterator = ifcSpace.getHasAssignments().iterator();
					while (relAssignIterator.hasNext()) {
						IfcRelAssigns relAssigns = relAssignIterator.next();
						if (relAssigns instanceof IfcRelAssignsToGroup) {
							IfcRelAssignsToGroup relAssignsToGroup = (IfcRelAssignsToGroup) relAssigns;
							if (relAssignsToGroup.getRelatingGroup() instanceof IfcZone) {
								IfcZone ifcZone = (IfcZone) relAssignsToGroup.getRelatingGroup();
								Resource resZone = rdfModel.createResource(
										rdfModel.getNsPrefixURI("om") + "Zone_" + ifcZone.getExpressId());
								resHeatExchanger.addProperty(
										ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "feeds"),
										resZone);
							}
						}
					}
				} else {
					log.warn("unsupported case");
				}
			}
			// add psets
			parsePsets(ifcHeatExchanger, resHeatExchanger);

		} else if (product instanceof IfcUnitaryControlElement) {
			IfcUnitaryControlElement ifcUnitaryControlElement = (IfcUnitaryControlElement) product;
			if (ifcUnitaryControlElement.getPredefinedType().equals(IfcUnitaryControlElementTypeEnum.THERMOSTAT)) {

				Resource resControlPanel = rdfModel
						.createResource(rdfModel.getNsPrefixURI("om") + getSerial(product) + "_Equipment");

				Resource resControlPanelRef = rdfModel.createResource();
				resControlPanelRef
						.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcName"),
								ifcUnitaryControlElement.getName() != null
										? ifcUnitaryControlElement.getName().getValue()
										: "Undefined");
				resControlPanelRef.addProperty(
						ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "ifcGlobalID"),
						ResourceFactory.createStringLiteral(ifcUnitaryControlElement.getGlobalId().getValue()));

				resControlPanel.addProperty(RDF.type,
						ResourceFactory.createResource(rdfModel.getNsPrefixURI("brick") + "Thermostat"));
				resControlPanel.addProperty(RDF.type,
						ResourceFactory.createResource(rdfModel.getNsPrefixURI("s4bldg") + "UnitaryControlElement"));
				resControlPanel.addProperty(
						ResourceFactory.createProperty(rdfModel.getNsPrefixURI("ref") + "hasExternalReference"),
						resControlPanelRef);

				resControlPanel.addProperty(
						ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "hasLocation"), resParent);
				resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("brick") + "isLocationOf"),
						resControlPanel);
				resParent.addProperty(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("s4bldg") + "contains"),
						resControlPanel);

				// add psets
				parsePsets(ifcUnitaryControlElement, resControlPanel);
			} else {
				log.warn("unsupported case11");
			}
		}
	}

	

	void parsePsets(IfcProduct ifcProduct, Resource resProduct) {
		Iterator<IfcRelDefinesByProperties> windowRelDefinesByProperties = ifcProduct.getIsDefinedBy().iterator();
		while (windowRelDefinesByProperties.hasNext()) {
			IfcRelDefinesByProperties relDefinesByProperties = windowRelDefinesByProperties.next();
			if (relDefinesByProperties.getRelatingPropertyDefinition() instanceof IfcPropertySet) {
				IfcPropertySet propertySet = (IfcPropertySet) relDefinesByProperties.getRelatingPropertyDefinition();
				Iterator<IfcProperty> propertiesIterator = propertySet.getHasProperties().iterator();
				while (propertiesIterator.hasNext()) {
					IfcProperty ifcProperty = propertiesIterator.next();
					if (ifcProperty instanceof IfcPropertySingleValue) {
						IfcPropertySingleValue propertySignleValue = (IfcPropertySingleValue) ifcProperty;
						if (propertySignleValue.getName().getValue().equalsIgnoreCase("Serial")) {
							IfcValue ifcValue = propertySignleValue.getNominalValue();
							if (ifcValue instanceof IfcIdentifier) {
								IfcIdentifier ifcIdentifier = (IfcIdentifier) ifcValue;
								resProduct.addLiteral(
										ResourceFactory
												.createProperty(rdfModel.getNsPrefixURI("s4ener") + "serialNumber"),
										ResourceFactory.createStringLiteral(ifcIdentifier.getValue()));
							}
							// if(ifcValue instanceof IfcText) {
							// IfcText ifcText = (IfcText) ifcValue;
							// resProduct.addLiteral(ResourceFactory.createProperty(rdfModel.getNsPrefixURI("s4ener")
							// + "serialNumber"), ResourceFactory.createStringLiteral(ifcText.getValue()));
							// }
							}
						
					}
				}
			} else {
				log.warn("unsupported case");
			}
		}
	}

	public String getSerial(IfcProduct ifcProduct) {
		Iterator<IfcRelDefinesByProperties> windowRelDefinesByProperties = ifcProduct.getIsDefinedBy().iterator();
		while (windowRelDefinesByProperties.hasNext()) {
			IfcRelDefinesByProperties relDefinesByProperties = windowRelDefinesByProperties.next();
			if (relDefinesByProperties.getRelatingPropertyDefinition() instanceof IfcPropertySet) {
				IfcPropertySet propertySet = (IfcPropertySet) relDefinesByProperties.getRelatingPropertyDefinition();
				Iterator<IfcProperty> propertiesIterator = propertySet.getHasProperties().iterator();
				while (propertiesIterator.hasNext()) {
					IfcProperty ifcProperty = propertiesIterator.next();
					if (ifcProperty instanceof IfcPropertySingleValue) {
						IfcPropertySingleValue propertySignleValue = (IfcPropertySingleValue) ifcProperty;
						if (propertySignleValue.getName().getValue().equalsIgnoreCase("Serial")) {
							IfcValue ifcValue = propertySignleValue.getNominalValue();
							// if(ifcValue instanceof IfcText) {
							// IfcText ifcText = (IfcText) ifcValue;
							// return ifcText.getValue();
							if (ifcValue instanceof IfcIdentifier) {
								IfcIdentifier ifcIdentifier = (IfcIdentifier) ifcValue;
								return ifcIdentifier.getValue();

							}
						}
					}
				}
			} else {
				log.warn("unsupported case");
			}
		}
		return null;
	}

}