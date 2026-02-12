use sea_orm::entity::prelude::*;

#[derive(Clone, Debug, PartialEq, DeriveEntityModel)]
#[sea_orm(table_name = "bill_line")]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: i64,
    pub bill_id: i64,
    pub line_no: i32,
    pub concept: String,
    pub quantity: Decimal,
    pub unit_amount: Decimal,
    pub line_amount: Decimal,
    pub created_at: DateTimeWithTimeZone,
}

#[derive(Copy, Clone, Debug, EnumIter, DeriveRelation)]
pub enum Relation {
    #[sea_orm(
        belongs_to = "super::bill::Entity",
        from = "Column::BillId",
        to = "super::bill::Column::Id",
        on_update = "Cascade",
        on_delete = "Cascade"
    )]
    Bill,
}

impl Related<super::bill::Entity> for Entity {
    fn to() -> RelationDef {
        Relation::Bill.def()
    }
}

impl ActiveModelBehavior for ActiveModel {}
