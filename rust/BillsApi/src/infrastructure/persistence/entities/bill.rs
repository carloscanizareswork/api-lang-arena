use sea_orm::entity::prelude::*;

#[derive(Clone, Debug, PartialEq, DeriveEntityModel)]
#[sea_orm(table_name = "bill")]
pub struct Model {
    #[sea_orm(primary_key)]
    pub id: i64,
    pub bill_number: String,
    pub issued_at: Date,
    pub customer_name: String,
    pub subtotal: Decimal,
    pub tax: Decimal,
    pub currency: String,
    pub created_at: DateTimeWithTimeZone,
}

#[derive(Copy, Clone, Debug, EnumIter, DeriveRelation)]
pub enum Relation {
    #[sea_orm(has_many = "super::bill_line::Entity")]
    BillLine,
}

impl Related<super::bill_line::Entity> for Entity {
    fn to() -> RelationDef {
        Relation::BillLine.def()
    }
}

impl ActiveModelBehavior for ActiveModel {}
